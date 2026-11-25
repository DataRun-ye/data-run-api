package org.nmcpye.datarun.characterization.a2;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.stubbing.Answer;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldDecisionResult;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldSubmissionDecisions;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.revision.JournalReconciliation;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionCommand;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleService;
import org.nmcpye.datarun.jpa.intake.HeldState;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.CurrentUserDetailsService;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The submission lock first, with real transactions. An upload on either route, and an admin delete, take the
 * submission lock as their first read, and their checks and their save use the submission it locked. Each test holds
 * one writer still between its reads (at its call into the persistence layer, inside its transaction), starts another
 * change of the same submission, and watches PostgreSQL: the other change waits on the submission lock. Then the first
 * writer goes on, and both finish, one after the other: no upload item fails, the delete is done, and the journal
 * records both changes in order.
 *
 * <p>Without the submission lock, the other change committed while the first writer was held, and the first writer's
 * save failed its version check: the upload item was answered {@code UNEXPECTED_FAILURE}, or the delete 409.</p>
 *
 * <p>The lock's bound: a holder that never lets go of the submission fails each writer waiting on it once the bound
 * passes (5 s, {@code datarun.submissions.lock-timeout}), and its connection returns to the pool.</p>
 *
 * <p>Not a golden: it reuses the A2 seed, through the production filter chain, with a pool of four connections, so
 * that the waiting writer waits on the submission lock and not for a connection.</p>
 */
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=4")
class SubmissionLockIT extends A2GoldenSupport {

    private static final long WAIT_SECONDS = 10;
    /** The submission lock's bound when nothing sets it, and what a stalled holder costs a writer at most. */
    private static final Duration BOUND = Duration.ofSeconds(5);
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SubmissionLockIT.class);
    /** The sessions whose locking read of a submission waits on a lock, as PostgreSQL shows them. */
    private static final String WAITING_ON_THE_SUBMISSION_LOCK = """
        SELECT count(*) FROM pg_stat_activity
        WHERE datname = current_database() AND state = 'active' AND wait_event_type = 'Lock'
          AND query ILIKE '%from data_submission%for%update%'""";

    @MockitoSpyBean
    private DataSubmissionService submissionService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private HeldSubmissionRepository heldSubmissions;

    @Autowired
    private GoLiveRuleService goLiveRuleService;

    @Autowired
    private DataSubmissionRepository submissionRepository;

    @Autowired
    private SubmissionUploads uploads;

    @Autowired
    private DomainUserDetailsService users;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentUserDetailsService currentUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private ExecutorService writers;
    private HeldStill held;

    @BeforeEach
    void startWriters() {
        writers = Executors.newCachedThreadPool();
        held = new HeldStill();
    }

    /** Lets a held writer go before the clean-up removes what it wrote, whatever the test found. */
    @AfterEach
    void stopWriters() throws Exception {
        held.release();
        writers.shutdown();
        assertThat(writers.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).as("the writers finished").isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Group.class, names = {"G3", "G0"})
    void anAdminDeleteWaitsForAnUploadBetweenItsReadsAndNeitherFails(Group group) throws Exception {
        String uid = uid("lockUp" + group.name());
        assertThat(phoneAnswer(upload(group, formA(group, uid, "first")))).isEqualTo(created(uid));
        doAnswer(held.thenTheRealMethod()).when(submissionService).upsert(withUid(uid), any(), any(), any());

        CompletableFuture<MockHttpServletResponse> upload = run(() -> upload(group, formA(group, uid, "changed")));
        held.awaitReached();
        CompletableFuture<MockHttpServletResponse> delete = run(() -> adminDelete(uid));
        boolean waited = waitsOnTheSubmissionLock(delete);
        held.release();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(waited)
                .as("the delete waited on the submission lock while the upload was between its reads").isTrue();
            soft.assertThat(phoneAnswer(get(upload))).as("the upload item's answer").isEqualTo(updated(uid));
            soft.assertThat(get(delete).getStatus()).as("the delete's status").isEqualTo(204);
            soft.assertThat(revisions(uid)).as("the journal, in order").containsExactly(
                "0 CREATE " + command(group), "1 UPDATE " + command(group), "2 DELETE ADMIN_DELETE");
            soft.assertThat(reconciliation(uid)).as("the journal against the submission").isEmpty();
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Group.class, names = {"G3", "G0"})
    void anUploadWaitsForAnAdminDeleteBetweenItsReadsAndNeitherFails(Group group) throws Exception {
        String uid = uid("lockDl" + group.name());
        assertThat(phoneAnswer(upload(group, formA(group, uid, "first")))).isEqualTo(created(uid));
        doAnswer(held.thenTheRealMethod()).when(submissionService).softDelete(withUid(uid), any());

        CompletableFuture<MockHttpServletResponse> delete = run(() -> adminDelete(uid));
        held.awaitReached();
        CompletableFuture<MockHttpServletResponse> upload = run(() -> upload(group, formA(group, uid, "changed")));
        boolean waited = waitsOnTheSubmissionLock(upload);
        held.release();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(waited)
                .as("the upload waited on the submission lock while the delete was between its reads").isTrue();
            soft.assertThat(get(delete).getStatus()).as("the delete's status").isEqualTo(204);
            // A copy sent after a delete changes the deleted submission and leaves it deleted, as without the race.
            soft.assertThat(phoneAnswer(get(upload))).as("the upload item's answer").isEqualTo(updated(uid));
            soft.assertThat(revisions(uid)).as("the journal, in order").containsExactly(
                "0 CREATE " + command(group), "1 DELETE ADMIN_DELETE", "2 UPDATE " + command(group));
            soft.assertThat(reconciliation(uid)).as("the journal against the submission").isEmpty();
        });
    }

    /**
     * Two upload items of one submission at once, from two phones or one phone twice, on either route: the second
     * waits on the submission lock for the first, and both are saved, one after the other.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Group.class, names = {"G3", "G0"})
    void anUploadWaitsForAnotherUploadOfTheSameSubmissionAndNeitherFails(Group waiting) throws Exception {
        String uid = uid("lockUu" + waiting.name());
        assertThat(phoneAnswer(upload(G3, formA(G3, uid, "first")))).isEqualTo(created(uid));
        doAnswer(held.thenTheRealMethod()).when(submissionService).upsert(withUid(uid), any(), any(), any());

        CompletableFuture<MockHttpServletResponse> first = run(() -> upload(G3, formA(G3, uid, "changed on a phone")));
        held.awaitReached();
        CompletableFuture<MockHttpServletResponse> second =
            run(() -> upload(waiting, formA(waiting, uid, "changed on another phone")));
        boolean waited = waitsOnTheSubmissionLock(second);
        held.release();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(waited)
                .as("the second upload waited on the submission lock while the first was between its reads").isTrue();
            soft.assertThat(phoneAnswer(get(first))).as("the first upload item's answer").isEqualTo(updated(uid));
            soft.assertThat(phoneAnswer(get(second))).as("the second upload item's answer").isEqualTo(updated(uid));
            soft.assertThat(revisions(uid)).as("the journal, in order").containsExactly(
                "0 CREATE UPLOAD", "1 UPDATE UPLOAD", "2 UPDATE " + command(waiting));
            soft.assertThat(reconciliation(uid)).as("the journal against the submission").isEmpty();
        });
    }

    /**
     * A held release whose replay finds its submission stored, because a live copy landed after the release checked
     * the uid (A2 S6), takes the submission lock; a phone's re-send meanwhile waits for it. The replay is rolled back
     * and its held copy superseded, then the re-send is saved: no upload item fails, and the release fails nothing.
     */
    @Test
    void anUploadWaitsForAHeldReleaseThatFoundItsSubmissionStoredAndNeitherFails() throws Exception {
        String uid = uid("lockRel");
        goLiveRules.put(A2Seed.ACTIVITY, null, null, Instant.parse("2099-01-01T00:00:00Z"), null, "a2-test");
        assertThat(phoneAnswer(upload(G3, formA(G3, uid, "held before the go-live")))).isEqualTo(created(uid));
        inTransaction(() -> jdbc.update("DELETE FROM go_live_rule WHERE activity_uid = :activity",
            Map.of("activity", A2Seed.ACTIVITY)));
        DomainUserDetailsService liveCopyLandsFirst = new DomainUserDetailsService(userRepository,
            currentUserDetailsService) {
            @Override
            public UserDetails loadUserByUsername(String login) {
                assertThat(phoneAnswer(uploadOrFail(formA(G3, uid, "live copy")))).isEqualTo(created(uid));
                return users.loadUserByUsername(login);
            }
        };
        HeldSubmissionDecisions decisions = new HeldSubmissionDecisions(heldSubmissions, goLiveRuleService,
            submissionRepository, uploads, liveCopyLandsFirst, objectMapper);
        doAnswer(held.thenTheRealMethod()).when(submissionService).upsert(withUid(uid), any(), any(),
            argThat(revision -> revision != null && revision.command() == RevisionCommand.HELD_RELEASE));
        SubmissionActor admin = SubmissionActor.of((CurrentUserDetails) users.loadUserByUsername(ADMIN));

        CompletableFuture<HeldDecisionResult> release = CompletableFuture.supplyAsync(() ->
            decisions.release(admin, new HeldDecisionRequest(List.of(uid), null, null, null)), writers);
        held.awaitReached();
        CompletableFuture<MockHttpServletResponse> resend =
            run(() -> upload(G3, formA(G3, uid, "re-sent while the release runs")));
        boolean waited = waitsOnTheSubmissionLock(resend);
        held.release();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(waited)
                .as("the re-send waited on the submission lock while the replay held it").isTrue();
            HeldDecisionResult decided = release.join();
            soft.assertThat(decided.superseded()).as("the held copy").containsExactly(uid);
            soft.assertThat(decided.failed()).as("the release's failures").isEmpty();
            soft.assertThat(phoneAnswer(get(resend))).as("the re-send's answer").isEqualTo(updated(uid));
            soft.assertThat(revisions(uid)).as("the journal: the live copy, then the re-send")
                .containsExactly("0 CREATE UPLOAD", "1 UPDATE UPLOAD");
            soft.assertThat(heldSubmissions.findByUid(uid)).map(row -> row.state())
                .contains(HeldState.SUPERSEDED);
            soft.assertThat(reconciliation(uid)).as("the journal against the submission").isEmpty();
        });
    }

    /**
     * A holder that never lets go of the submission (a session that locked it and stays open, as psql would): an
     * upload item of it fails once the bound passes, as any unexpected failure does, with one WARN line and no ERROR,
     * and the rest of its batch is saved. While the holder still holds, another submission's upload is served at once.
     */
    @Test
    @Timeout(30)
    void aStalledHolderFailsItsSubmissionsItemAfterTheBoundAndTheBatchGoesOn() throws Exception {
        String stalled = uid("stallUp");
        String alongside = uid("stallNew");
        String after = uid("stallAft");
        assertThat(phoneAnswer(upload(G3, formA(G3, stalled, "first")))).isEqualTo(created(stalled));
        ListAppender<ILoggingEvent> lines = capture(SubmissionUploads.class);
        try (Holder holder = holdTheSubmission(stalled)) {
            Timed batch = getTimed(timed(() ->
                upload(G3, formA(G3, stalled, "changed in a batch"), formA(G3, alongside, "new in the batch"))));
            Timed meanwhile = getTimed(timed(() -> upload(G3, formA(G3, after, "while the holder holds"))));
            log.info("A stalled holder: a batch with its upload item took {} ms; another submission's upload "
                + "meanwhile took {} ms", batch.took().toMillis(), meanwhile.took().toMillis());

            assertThat(phoneAnswer(batch.response())).isEqualTo("200 created=[\"" + alongside + "\"] updated=[] "
                + "failed={\"" + stalled + "\":\"" + SubmissionUploads.UNEXPECTED_FAILURE + "\"}");
            assertThat(batch.took()).as("the item's wait").isBetween(BOUND, BOUND.plusSeconds(3));
            assertThat(phoneAnswer(meanwhile.response())).isEqualTo(created(after));
            assertThat(meanwhile.took()).as("served at once").isLessThan(Duration.ofSeconds(2));
            assertThat(lines.list).singleElement().satisfies(line -> {
                assertThat(line.getLevel()).isEqualTo(Level.WARN);
                assertThat(line.getFormattedMessage())
                    .startsWith("Submission " + stalled + " (form " + FORM_A + ") from user ")
                    .contains("another change held it past the submission lock's bound")
                    .contains("SQLState 55P03");
            });
        } finally {
            release(SubmissionUploads.class, lines);
        }
        assertThat(revisions(stalled)).as("the stalled submission is unchanged").containsExactly("0 CREATE UPLOAD");
        assertThat(reconciliation(stalled)).isEmpty();
    }

    /**
     * Three upload items of a stalled submission at once, on three of the pool's four connections: the first waiter
     * fails once the bound passes; one queued behind it can wait up to twice the bound, since PostgreSQL bounds each
     * lock wait on its own (the queue on the submission, then its holder). While they wait, the fourth connection
     * serves another submission's upload at once, and every connection comes back.
     */
    @Test
    @Timeout(30)
    void writersQueuedOnAStalledHolderEachFailWithinTwiceTheBoundAndThePoolKeepsServing() throws Exception {
        String stalled = uid("stallQue");
        String after = uid("stallSrv");
        assertThat(phoneAnswer(upload(G3, formA(G3, stalled, "first")))).isEqualTo(created(stalled));
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        try (Holder holder = holdTheSubmission(stalled)) {
            List<CompletableFuture<Timed>> resends = List.of(
                timed(() -> upload(G3, formA(G3, stalled, "changed once"))),
                timed(() -> upload(G3, formA(G3, stalled, "changed twice"))),
                timed(() -> upload(G3, formA(G3, stalled, "changed thrice"))));
            assertThat(waitingOnTheSubmissionLock(3)).as("three writers wait on the submission").isTrue();
            Timed meanwhile = getTimed(timed(() -> upload(G3, formA(G3, after, "while three wait"))));
            assertThat(resends).as("still waiting when the other upload was served")
                .noneMatch(CompletableFuture::isDone);
            List<Timed> failed = resends.stream().map(SubmissionLockIT::getTimed).toList();
            log.info("A stalled holder: three queued upload items failed after {} ms; another submission's upload "
                    + "meanwhile took {} ms", failed.stream().map(timed -> timed.took().toMillis()).toList(),
                meanwhile.took().toMillis());

            assertThat(phoneAnswer(meanwhile.response())).isEqualTo(created(after));
            assertThat(meanwhile.took()).as("served at once").isLessThan(Duration.ofSeconds(2));
            assertThat(failed).allSatisfy(timed -> {
                assertThat(phoneAnswer(timed.response())).isEqualTo("200 created=[] updated=[] failed={\"" + stalled
                    + "\":\"" + SubmissionUploads.UNEXPECTED_FAILURE + "\"}");
                assertThat(timed.took()).isBetween(BOUND, BOUND.multipliedBy(2).plusSeconds(3));
            });
            assertThat(failed.stream().map(Timed::took).min(Duration::compareTo).orElseThrow())
                .as("the first waiter's wait").isLessThan(BOUND.plusSeconds(3));
            assertThat(pool.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
        }
        assertThat(revisions(stalled)).as("the stalled submission is unchanged").containsExactly("0 CREATE UPLOAD");
    }

    /**
     * A holder that never lets go of the submission: the admin delete is refused once the bound passes, 409
     * {@code error.concurrencyFailure}, with one WARN line naming the submission, and nothing is written.
     */
    @Test
    void aStalledHolderMakesAnAdminDeleteAnswer409AfterTheBound() throws Exception {
        String stalled = uid("stallDel");
        assertThat(phoneAnswer(upload(G3, formA(G3, stalled, "first")))).isEqualTo(created(stalled));
        ListAppender<ILoggingEvent> lines = capture(SubmissionCommandService.class);
        try (Holder holder = holdTheSubmission(stalled)) {
            Timed refused = getTimed(timed(() -> adminDelete(stalled)));
            log.info("A stalled holder: the admin delete was refused after {} ms", refused.took().toMillis());

            assertThat(refused.response().getStatus()).isEqualTo(409);
            assertThat(json.readTree(refused.response().getContentAsString(StandardCharsets.UTF_8))
                .path("message").asText()).isEqualTo("error.concurrencyFailure");
            assertThat(refused.took()).isBetween(BOUND, BOUND.plusSeconds(4));
            assertThat(lines.list).singleElement().satisfies(line -> {
                assertThat(line.getLevel()).isEqualTo(Level.WARN);
                assertThat(line.getFormattedMessage())
                    .startsWith("Submission " + stalled + " not deleted by user ")
                    .contains("another change held it past the submission lock's bound")
                    .contains("SQLState 55P03");
            });
        } finally {
            release(SubmissionCommandService.class, lines);
        }
        assertThat(revisions(stalled)).as("nothing deleted").containsExactly("0 CREATE UPLOAD");
        assertThat(reconciliation(stalled)).isEmpty();
    }

    private static Item formA(Group group, String uid, String name) {
        return item(group, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(group, uid, name, "r1", "s1", 1));
    }

    private static String command(Group group) {
        return group.versioned() ? "UPLOAD" : "LEGACY_UPLOAD";
    }

    private static DataSubmission withUid(String uid) {
        return argThat(submission -> submission != null && uid.equals(submission.getUid()));
    }

    private MockHttpServletResponse upload(Group group, Item... items) throws Exception {
        MockHttpServletRequestBuilder request = post(group.route())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, FIELD_1))
            .content(json.writeValueAsString(A2Payloads.body(items)));
        group.headers().forEach(request::header);
        return mvc.perform(request).andReturn().getResponse();
    }

    private MockHttpServletResponse uploadOrFail(Item item) {
        try {
            return upload(G3, item);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MockHttpServletResponse adminDelete(String uid) throws Exception {
        return mvc.perform(delete("/api/v1/dataSubmission/" + uid)
                .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, ADMIN)))
            .andReturn().getResponse();
    }

    private CompletableFuture<MockHttpServletResponse> run(Callable<MockHttpServletResponse> request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return request.call();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }, writers);
    }

    /** A request's answer and how long it took. */
    private record Timed(MockHttpServletResponse response, Duration took) {
    }

    private CompletableFuture<Timed> timed(Callable<MockHttpServletResponse> request) {
        return CompletableFuture.supplyAsync(() -> {
            long started = System.nanoTime();
            try {
                MockHttpServletResponse response = request.call();
                return new Timed(response, Duration.ofNanos(System.nanoTime() - started));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }, writers);
    }

    private static Timed getTimed(CompletableFuture<Timed> timed) {
        try {
            return timed.get(BOUND.multipliedBy(2).toSeconds() + WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A session of its own that locks the submission and holds it until closed, as an open psql transaction would. */
    private Holder holdTheSubmission(String uid) throws Exception {
        Connection holder = connect();
        holder.setAutoCommit(false);
        try (PreparedStatement lock = holder.prepareStatement(
            "SELECT 1 FROM data_submission WHERE uid = ? FOR UPDATE")) {
            lock.setString(1, uid);
            try (ResultSet locked = lock.executeQuery()) {
                assertThat(locked.next()).as("the holder locked the submission").isTrue();
            }
        }
        return new Holder(holder);
    }

    /** Lets go of the submission: rolls the holder's transaction back. */
    private record Holder(Connection connection) implements AutoCloseable {
        @Override
        public void close() throws SQLException {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    private Connection connect() throws SQLException {
        // A connection of its own: the pool's are the writers'.
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        return DriverManager.getConnection(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
    }

    private static ListAppender<ILoggingEvent> capture(Class<?> type) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(type)).addAppender(appender);
        return appender;
    }

    private static void release(Class<?> type, ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(type)).detachAppender(appender);
        appender.stop();
    }

    private static MockHttpServletResponse get(CompletableFuture<MockHttpServletResponse> response) {
        try {
            return response.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** What the phone applies for its upload item: the status and the three lists. */
    private String phoneAnswer(MockHttpServletResponse response) {
        try {
            JsonNode answer = json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
            return response.getStatus() + " created=" + answer.path("created") + " updated=" + answer.path("updated")
                + " failed=" + answer.path("failed");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String created(String uid) {
        return "200 created=[\"" + uid + "\"] updated=[] failed={}";
    }

    private static String updated(String uid) {
        return "200 created=[] updated=[\"" + uid + "\"] failed={}";
    }

    private List<String> revisions(String uid) {
        return jdbc.query("""
                SELECT r.version, r.kind, r.command FROM journal.submission_revision r
                JOIN data_submission s ON s.id = r.submission_id
                WHERE s.uid = :uid ORDER BY r.version""",
            Map.of("uid", uid),
            (result, n) -> result.getLong("version") + " " + result.getString("kind") + " "
                + result.getString("command"));
    }

    private List<String> reconciliation(String uid) {
        return JournalReconciliation.findings(jdbc, "s.uid = :uid", Map.of("uid", uid));
    }

    /** Whether, within {@link #WAIT_SECONDS}, this many sessions wait on the submission lock at once. */
    private boolean waitingOnTheSubmissionLock(int writers) throws Exception {
        try (Connection watcher = connect();
             PreparedStatement waiting = watcher.prepareStatement(WAITING_ON_THE_SUBMISSION_LOCK)) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            while (System.nanoTime() < deadline) {
                try (ResultSet count = waiting.executeQuery()) {
                    if (count.next() && count.getInt(1) >= writers) {
                        return true;
                    }
                }
                Thread.sleep(20);
            }
        }
        return false;
    }

    /**
     * Whether the other writer comes to wait on the submission lock (its locking read waits, as PostgreSQL shows in
     * {@code pg_stat_activity}) while still unfinished; false when it finishes first, or never waits.
     */
    private boolean waitsOnTheSubmissionLock(CompletableFuture<?> other) throws Exception {
        try (Connection watcher = connect();
             PreparedStatement waiting = watcher.prepareStatement(WAITING_ON_THE_SUBMISSION_LOCK)) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            while (System.nanoTime() < deadline && !other.isDone()) {
                try (ResultSet count = waiting.executeQuery()) {
                    if (count.next() && count.getInt(1) > 0) {
                        return !other.isDone();
                    }
                }
                Thread.sleep(20);
            }
        }
        return false;
    }

    /**
     * Holds the first call it answers still, inside the caller's transaction, until released; then, and for every
     * later call, the real method runs.
     */
    private static final class HeldStill {
        private final CountDownLatch reached = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        private final AtomicBoolean taken = new AtomicBoolean();

        Answer<Object> thenTheRealMethod() {
            return invocation -> {
                if (taken.compareAndSet(false, true)) {
                    reached.countDown();
                    if (!released.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("The held writer was never released");
                    }
                }
                return invocation.callRealMethod();
            };
        }

        void awaitReached() throws InterruptedException {
            assertThat(reached.await(WAIT_SECONDS, TimeUnit.SECONDS))
                .as("the held writer reached the persistence layer").isTrue();
        }

        void release() {
            released.countDown();
        }
    }
}
