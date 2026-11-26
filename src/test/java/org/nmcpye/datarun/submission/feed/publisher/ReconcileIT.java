package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.outbox.processing.ChangeBacklog;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * Reconcile repairs the feed once the first publication has completed (engineering, 2026-11-26): every head the feed
 * lacks or holds at another version is published behind the live backlog, after a period with the feed off and after
 * dead changes, and a pass restarted midway skips what it already wrote. Before the first publication has completed,
 * it publishes none the feed lacks and counts them, while live changes publish. A scan reads seven submissions and a
 * batch publishes five in these tests. The database is shared with other tests: the assertions on records cover this
 * test's submissions, and a pass also publishes whatever else the database holds.
 */
@FeedPublisherIntegrationTest
class ReconcileIT {

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private SubmissionPublisher publisher;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ActivationSwitches switches;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeedPublisherProperties properties;

    @Autowired
    private StoredSubmissionJdbcReader storedSubmissions;

    @Autowired
    private ChangeBacklog backlog;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private FeedFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
        // the pass requested at this context's start, over whatever the shared database held, runs out first
        publisher.reconcileNightly();
        runUntilIdle(publisher);
        fixture.clean();
    }

    @AfterEach
    void tearDown() {
        fixture.unfixPlacement();
        fixture.clean();
    }

    private void drain() {
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // every claimed row ends success, failed with a later retry, or dead
        }
    }

    private static int runUntilIdle(SubmissionPublisher publisher) {
        int batches = 0;
        while (publisher.runBatch()) {
            batches++;
        }
        return batches;
    }

    /** Marks the submissions' outbox rows done, as a release without the feed (or with it off) would leave them. */
    private void processedWithTheFeedOff() {
        fixture.execute("UPDATE outbox SET status = 'success' WHERE status = 'pending'", Map.of());
    }

    /** A publisher as switching the feed on makes it: the switch is read at start, which requests a pass. */
    private SubmissionPublisher switchedOn() {
        SubmissionPublisher started = new SubmissionPublisher(switches, dataSource, transactionManager, properties,
            new FeedWriter(new FormDefinitions(), objectMapper, SplitPartners.NONE), storedSubmissions, backlog,
            new SimpleMeterRegistry());
        started.reconcileAtStart();
        return started;
    }

    private Map<String, Object> freshness() {
        return jdbc.queryForMap("""
            SELECT unpublished, last_reconciled_at, first_publication_completed_at
            FROM feed.freshness WHERE feed = 'submissions'""", Map.of());
    }

    /** This test's submissions whose head the feed lacks or holds at another version. */
    private long unpublishedHeads() {
        return fixture.count("""
            SELECT count(*) FROM data_submission s
            JOIN LATERAL (SELECT max(version) AS version FROM journal.submission_revision r
                          WHERE r.submission_id = s.id) h ON h.version IS NOT NULL
            LEFT JOIN feed.submission f ON f.id = s.id
            WHERE f.version IS DISTINCT FROM h.version AND s.id LIKE :prefix""",
            Map.of("prefix", FeedFixture.ID_PREFIX + "%"));
    }

    /**
     * Switched on before any first publication: reconcile publishes none of the submissions the feed lacks, and counts
     * them in {@code unpublished}; it still repairs a submission the feed holds at another version, and live changes
     * publish as they happen.
     */
    @Test
    void beforeTheFirstPublicationSwitchOnLeavesWhatTheFeedLacksToItCountedWhileLiveChangesPublish() {
        Ids behind = fixture.create(document("behind v0", 1, row("r1", "a")));
        drain();
        fixture.update(behind, document("behind v1", 1, row("r1", "a2")));
        List<Ids> lacked = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            lacked.add(fixture.create(document("lacked " + i, i, row("r" + i, "x"))));
        }
        processedWithTheFeedOff();

        SubmissionPublisher started = switchedOn();
        runUntilIdle(started);

        for (Ids submission : lacked) {
            assertThat(fixture.record(submission)).as("left to the first publication: %s", submission.id()).isEmpty();
        }
        assertThat(fixture.record(behind)).as("a head the feed holds at another version is repaired")
            .containsEntry("version", 1L);
        Map<String, Object> freshness = freshness();
        assertThat((Long) freshness.get("unpublished")).as("the six the feed lacks and the one behind, at least")
            .isGreaterThanOrEqualTo(7);
        assertThat(freshness.get("last_reconciled_at")).isNotNull();
        assertThat(freshness.get("first_publication_completed_at")).as("set by the first publication only").isNull();

        Ids live = fixture.create(document("live", 1, row("r1", "b")));
        drain();
        assertThat(fixture.record(live)).as("a live change publishes").containsEntry("version", 0L);
        assertThat(fixture.rows(live)).hasSize(1);
        for (Ids submission : lacked) {
            assertThat(fixture.record(submission)).as("still left: %s", submission.id()).isEmpty();
        }
    }

    /** After the first publication and a period with the feed off, switching it on reconciles as before. */
    @Test
    void afterTheFirstPublicationAndAPeriodWithTheFeedOffSwitchOnPublishesEveryHeadTheFeedLacks() {
        fixture.firstPublicationCompleted();
        Ids current = fixture.create(document("published", 1, row("r1", "a")));
        drain();
        Ids behind = fixture.create(document("behind v0", 1, row("r1", "a")));
        drain();
        fixture.update(behind, document("behind v1", 1, row("r1", "a2")));
        List<Ids> missing = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Ids submission = fixture.create(document("missing " + i, i, row("r" + i, "x")));
            if (i % 3 == 0) {
                fixture.update(submission, document("missing again " + i, i, row("r" + i, "y")));
            }
            missing.add(submission);
        }
        processedWithTheFeedOff();
        Map<String, Object> currentRecord = fixture.record(current);

        SubmissionPublisher started = switchedOn();
        int batches = runUntilIdle(started);

        assertThat(batches).as("published in batches of five, behind the live backlog").isGreaterThanOrEqualTo(3);
        for (Ids submission : missing) {
            assertThat(fixture.record(submission)).as(submission.id())
                .containsEntry("version", fixture.head(submission));
            assertThat(fixture.rows(submission)).hasSize(1);
        }
        assertThat(fixture.record(behind)).containsEntry("version", 1L);
        assertThat(fixture.record(current)).as("a head already held is left as it was").isEqualTo(currentRecord);
        assertThat(unpublishedHeads()).isZero();
        Map<String, Object> freshness = freshness();
        assertThat((Long) freshness.get("unpublished")).as("the eleven missing and the one behind, at least")
            .isGreaterThanOrEqualTo(12);
        assertThat(freshness.get("last_reconciled_at")).isNotNull();
    }

    /**
     * Dead changes, of both kinds: an outbox row the ETL gave up on, whose change never reached the feed, and a pending
     * change past the cap. Reconcile tries the dead pending change at each pass, keeps it dead while the cause stays,
     * and publishes it once the cause is fixed.
     */
    @Test
    void afterDeadChangesReconcilePublishesThemOnceTheirCauseIsFixed() {
        fixture.firstPublicationCompleted();
        Ids deadOutboxRow = fixture.create(document("dead row", 1, row("r1", "a")));
        fixture.execute("UPDATE outbox SET status = 'dead', last_error = 'given up' WHERE submission_id = :id",
            Map.of("id", deadOutboxRow.id()));
        Ids unplaced = FeedFixture.nextIds();
        fixture.create(unplaced, document("unplaced", 1, row("r1", "a")), VERSION_2_UID, MISSING_ASSIGNMENT_UID);
        drain();
        runUntilIdle(publisher);
        assertThat(fixture.pending(unplaced)).containsEntry("attempts", 3);
        assertThat(fixture.pending(unplaced).get("dead_at")).isNotNull();

        publisher.reconcileNightly();
        runUntilIdle(publisher);
        assertThat(fixture.record(deadOutboxRow)).as("the dead outbox row's change").containsEntry("version", 0L);
        assertThat(fixture.pending(unplaced)).as("tried again, and still dead").containsEntry("attempts", 4);
        assertThat(fixture.pending(unplaced).get("dead_at")).isNotNull();

        fixture.fixPlacement();
        publisher.reconcileNightly();
        runUntilIdle(publisher);
        assertThat(fixture.record(unplaced)).containsEntry("version", 0L);
        assertThat(fixture.pending(unplaced)).isEmpty();
        assertThat(unpublishedHeads()).isZero();
    }

    @Test
    void aPendingChangeThatIsNotDeadIsLeftToItsRetries() {
        Ids unplaced = FeedFixture.nextIds();
        fixture.create(unplaced, document("unplaced", 1), VERSION_2_UID, MISSING_ASSIGNMENT_UID);
        drain();
        fixture.execute("UPDATE feed_ops.pending SET next_attempt_at = now() + interval '1 hour'", Map.of());
        fixture.fixPlacement();

        publisher.reconcileNightly();
        runUntilIdle(publisher);

        assertThat(fixture.pending(unplaced)).as("not tried by reconcile").containsEntry("attempts", 1);
        assertThat(fixture.record(unplaced)).isEmpty();
        assertThat((Long) freshness().get("unpublished")).as("but counted").isGreaterThanOrEqualTo(1);
    }

    /**
     * A restart midway through a pass: the next process starts a new pass, which skips every head the first one
     * wrote (their positions stay) and publishes the rest.
     */
    @Test
    void aPassRestartedMidwaySkipsWhatTheFirstPassWrote() {
        fixture.firstPublicationCompleted();
        List<Ids> submissions = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            submissions.add(fixture.create(document("s" + i, i, row("r" + i, "x"))));
        }
        processedWithTheFeedOff();
        publisher.reconcileNightly();
        assertThat(publisher.runBatch()).isTrue();
        Map<String, Long> writtenBeforeTheRestart = new java.util.HashMap<>();
        for (Ids submission : submissions) {
            Object position = fixture.record(submission).get("position");
            if (position != null) {
                writtenBeforeTheRestart.put(submission.id(), (Long) position);
            }
        }
        assertThat(writtenBeforeTheRestart).as("at most one batch of five, before the restart")
            .isNotEmpty().hasSizeLessThanOrEqualTo(5);

        SubmissionPublisher restarted = switchedOn();
        runUntilIdle(restarted);

        for (Ids submission : submissions) {
            Map<String, Object> record = fixture.record(submission);
            assertThat(record).as(submission.id()).containsEntry("version", 0L);
            Long before = writtenBeforeTheRestart.get(submission.id());
            if (before != null) {
                assertThat(record.get("position")).as("written once").isEqualTo(before);
            }
        }
        assertThat(unpublishedHeads()).isZero();
    }
}
