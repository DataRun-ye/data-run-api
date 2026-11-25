package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.stubbing.Answer;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.revision.JournalReconciliation;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
 * <p>Not a golden: it reuses the A2 seed, through the production filter chain, with a pool of four connections, so
 * that the waiting writer waits on the submission lock and not for a connection.</p>
 */
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=4")
class SubmissionLockIT extends A2GoldenSupport {

    private static final long WAIT_SECONDS = 10;

    @MockitoSpyBean
    private DataSubmissionService submissionService;

    @Autowired
    private DataSource dataSource;

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

    private static Item formA(Group group, String uid, String name) {
        return item(group, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(group, uid, name, "r1", "s1", 1));
    }

    private static String command(Group group) {
        return group.versioned() ? "UPLOAD" : "LEGACY_UPLOAD";
    }

    private static DataSubmission withUid(String uid) {
        return argThat(submission -> submission != null && uid.equals(submission.getUid()));
    }

    private MockHttpServletResponse upload(Group group, Item item) throws Exception {
        MockHttpServletRequestBuilder request = post(group.route())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, FIELD_1))
            .content(json.writeValueAsString(A2Payloads.body(item)));
        group.headers().forEach(request::header);
        return mvc.perform(request).andReturn().getResponse();
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

    /**
     * Whether the other writer comes to wait on the submission lock (its locking read waits, as PostgreSQL shows in
     * {@code pg_stat_activity}) while still unfinished; false when it finishes first, or never waits.
     */
    private boolean waitsOnTheSubmissionLock(CompletableFuture<?> other) throws Exception {
        // A connection of its own: the pool's are the writers'.
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        try (Connection watcher = DriverManager.getConnection(pool.getJdbcUrl(), pool.getUsername(),
            pool.getPassword());
             PreparedStatement waiting = watcher.prepareStatement("""
                 SELECT count(*) FROM pg_stat_activity
                 WHERE datname = current_database() AND state = 'active' AND wait_event_type = 'Lock'
                   AND query ILIKE '%from data_submission%for%update%'""")) {
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
