package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
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
 * Reconcile: every head the feed lacks or holds at another version is published behind the live backlog, after a
 * period with the feed off and after dead changes, and a pass restarted midway skips what it already wrote. A scan
 * reads seven submissions and a batch publishes five in these tests. The database is shared with other tests: the
 * assertions on records cover this test's submissions, and the pass also publishes whatever else the database holds,
 * which is expected to publish cleanly ({@link #othersUnpublishable} says when it does not).
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

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private FeedFixture fixture;

    /** Submissions of other tests whose heads the feed cannot publish: none, unless another test left one behind. */
    private long othersUnpublishable;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
        // the pass requested at this context's start, over whatever the shared database held, runs out first
        publisher.reconcileNightly();
        runUntilIdle(publisher);
        othersUnpublishable = unpublishedHeads(false);
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

    private Map<String, Object> freshness() {
        return jdbc.queryForMap("""
            SELECT unpublished, last_reconciled_at, first_publication_completed_at
            FROM feed.freshness WHERE feed = 'submissions'""", Map.of());
    }

    /** This test's submissions (or the others) whose head the feed lacks or holds at another version. */
    private long unpublishedHeads(boolean fixtures) {
        return fixture.count("""
            SELECT count(*) FROM data_submission s
            JOIN LATERAL (SELECT max(version) AS version FROM journal.submission_revision r
                          WHERE r.submission_id = s.id) h ON h.version IS NOT NULL
            LEFT JOIN feed.submission f ON f.id = s.id
            WHERE f.version IS DISTINCT FROM h.version AND (s.id LIKE :prefix) = :fixtures""",
            Map.of("prefix", FeedFixture.ID_PREFIX + "%", "fixtures", fixtures));
    }

    @Test
    void afterAPeriodWithTheFeedOffReconcilePublishesEveryHeadTheFeedLacks() {
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

        publisher.reconcileNightly();
        int batches = runUntilIdle(publisher);

        assertThat(batches).as("published in batches of five, behind the live backlog").isGreaterThanOrEqualTo(3);
        for (Ids submission : missing) {
            assertThat(fixture.record(submission)).as(submission.id())
                .containsEntry("version", fixture.head(submission));
            assertThat(fixture.rows(submission)).hasSize(1);
        }
        assertThat(fixture.record(behind)).containsEntry("version", 1L);
        assertThat(fixture.record(current)).as("a head already held is left as it was").isEqualTo(currentRecord);
        assertThat(unpublishedHeads(true)).isZero();
        Map<String, Object> freshness = freshness();
        assertThat((Long) freshness.get("unpublished")).as("the eleven missing and the one behind, at least")
            .isGreaterThanOrEqualTo(12);
        assertThat(freshness.get("last_reconciled_at")).isNotNull();
        assertThat(freshness.get("first_publication_completed_at") != null)
            .as("set once a pass has written every head it found unpublished (other tests' submissions that cannot "
                + "be published: %d)", othersUnpublishable)
            .isEqualTo(othersUnpublishable == 0);
    }

    /**
     * Dead changes, of both kinds: an outbox row the ETL gave up on, whose change never reached the feed, and a pending
     * change past the cap. Reconcile tries the dead pending change at each pass, keeps it dead while the cause stays,
     * and publishes it once the cause is fixed.
     */
    @Test
    void afterDeadChangesReconcilePublishesThemOnceTheirCauseIsFixed() {
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
        assertThat(freshness().get("first_publication_completed_at")).as("a head the pass could not write")
            .isNull();

        fixture.fixPlacement();
        publisher.reconcileNightly();
        runUntilIdle(publisher);
        assertThat(fixture.record(unplaced)).containsEntry("version", 0L);
        assertThat(fixture.pending(unplaced)).isEmpty();
        assertThat(unpublishedHeads(true)).isZero();
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

        SubmissionPublisher restarted = new SubmissionPublisher(switches, dataSource, transactionManager, properties,
            new FeedWriter(new FormDefinitions(), objectMapper, SplitPartners.NONE), new SimpleMeterRegistry());
        restarted.reconcileAtStart();
        runUntilIdle(restarted);

        for (Ids submission : submissions) {
            Map<String, Object> record = fixture.record(submission);
            assertThat(record).as(submission.id()).containsEntry("version", 0L);
            Long before = writtenBeforeTheRestart.get(submission.id());
            if (before != null) {
                assertThat(record.get("position")).as("written once").isEqualTo(before);
            }
        }
        assertThat(unpublishedHeads(true)).isZero();
    }
}
