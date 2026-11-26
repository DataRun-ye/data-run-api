package org.nmcpye.datarun.submission.feed.publisher;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.admin.EtlRunLock;
import org.nmcpye.datarun.etl.admin.ScheduledOrchestratorRunner;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.metrics.EtlHeartbeat;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * The freshness record counts what waits at each poll, and the ETL's heartbeat follows the feed's freshness target (two
 * minutes): it stops for an outbox change or a pending change older than the target, and for anything dead, and
 * resumes once the feed is within its target again. The polls are the application's ETL runner's, with its heartbeat
 * observed. Values are invented.
 */
@FeedPublisherIntegrationTest
class FreshnessIT {

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
    private EtlProperties etlProperties;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private FeedFixture fixture;
    private final EtlHeartbeat heartbeat = mock(EtlHeartbeat.class);
    private ScheduledOrchestratorRunner runner;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
        runner = new ScheduledOrchestratorRunner(jdbc, orchestrator, new EtlRunLock(dataSource),
            new EtlMetrics(new SimpleMeterRegistry(), dataSource, etlProperties), etlProperties, heartbeat,
            List.of(publisher));
        // whatever the shared database held is published, so this test's polls see only what it adds
        publisher.reconcileNightly();
        runner.scheduledRun();
        fixture.clean();
        clearInvocations(heartbeat);
    }

    @AfterEach
    void tearDown() {
        fixture.unfixPlacement();
        fixture.clean();
    }

    private Map<String, Object> freshness() {
        return jdbc.queryForMap("""
            SELECT backlog, pending, dead, oldest_unpublished_at IS NOT NULL AS has_oldest_unpublished,
                   oldest_pending_at IS NOT NULL AS has_oldest_pending, checked_at
            FROM feed.freshness WHERE feed = 'submissions'""", Map.of());
    }

    @Test
    void aPollWithNothingWaitingWritesFreshnessAndPings() {
        runner.scheduledRun();

        assertThat(freshness()).containsEntry("backlog", 0L).containsEntry("pending", 0L).containsEntry("dead", 0L)
            .containsEntry("has_oldest_unpublished", false).containsEntry("has_oldest_pending", false);
        verify(heartbeat).pollSucceeded();
    }

    /**
     * A failure that recurs on every retry: one poll processes the change (pending), retries it to the cap (three
     * attempts in these tests) and finds it dead, so no heartbeat; once the cause is fixed, reconcile publishes it, as
     * it does after the first publication, and the heartbeat resumes.
     */
    @Test
    void aFailureThatRecursOnEveryRetryGoesDeadAndStopsTheHeartbeatUntilReconcileFixesIt() {
        fixture.firstPublicationCompleted();
        Ids unplaced = FeedFixture.nextIds();
        fixture.create(unplaced, document("unplaced", 1, row("r1", "a")), VERSION_2_UID, MISSING_ASSIGNMENT_UID);

        runner.scheduledRun();

        assertThat(fixture.pending(unplaced)).containsEntry("attempts", 3);
        assertThat(fixture.pending(unplaced).get("dead_at")).isNotNull();
        assertThat(freshness()).containsEntry("dead", 1L).containsEntry("pending", 0L).containsEntry("backlog", 0L);
        verify(heartbeat, never()).pollSucceeded();

        fixture.fixPlacement();
        publisher.reconcileNightly();
        runner.scheduledRun();

        assertThat(fixture.record(unplaced)).containsEntry("version", 0L);
        assertThat(freshness()).containsEntry("dead", 0L);
        verify(heartbeat).pollSucceeded();
    }

    /**
     * Freshness counts the feed's own state only: a change tall or events fail on, and even give up on, is published by
     * the feed, so it is neither in the feed's backlog nor dead, and the heartbeat goes on.
     */
    @Test
    void aTallFailureLeavesNoBacklogNothingDeadAndTheHeartbeatSent() {
        Ids submission = fixture.create(document("tall fails", 1, row("r1", "a")));
        fixture.failLegacyWrites("tall_canonical");

        runner.scheduledRun();

        assertThat(fixture.outboxStatuses(submission)).containsExactly("failed");
        assertThat(fixture.record(submission)).containsEntry("version", 0L);
        assertThat(freshness()).containsEntry("backlog", 0L).containsEntry("dead", 0L)
            .containsEntry("has_oldest_unpublished", false);
        verify(heartbeat).pollSucceeded();

        clearInvocations(heartbeat);
        fixture.execute("UPDATE outbox SET status = 'dead' WHERE submission_id = :id", Map.of("id", submission.id()));
        runner.scheduledRun();

        assertThat(freshness()).as("a dead outbox row whose version the feed holds").containsEntry("backlog", 0L)
            .containsEntry("dead", 0L);
        verify(heartbeat).pollSucceeded();
    }

    @Test
    void anOutboxChangeOlderThanTheTargetStopsTheHeartbeat() {
        Ids waiting = fixture.create(document("waiting", 1));
        fixture.execute("""
            UPDATE outbox SET created_at = timezone('UTC', now()) - interval '3 minutes',
                              next_attempt_at = localtimestamp + interval '1 hour'
            WHERE submission_id = :id""", Map.of("id", waiting.id()));

        runner.scheduledRun();

        assertThat(freshness()).containsEntry("backlog", 1L).containsEntry("has_oldest_unpublished", true);
        verify(heartbeat, never()).pollSucceeded();

        fixture.endOutboxWaits(waiting);
        runner.scheduledRun();
        assertThat(freshness()).containsEntry("backlog", 0L);
        verify(heartbeat).pollSucceeded();
    }

    @Test
    void aPendingChangeOlderThanTheTargetStopsTheHeartbeat() {
        Ids unplaced = FeedFixture.nextIds();
        fixture.create(unplaced, document("unplaced", 1), VERSION_2_UID, MISSING_ASSIGNMENT_UID);
        orchestrator.runOnce("feed-test", 10);
        fixture.execute("""
            UPDATE feed_ops.pending SET first_failed_at = now() - interval '3 minutes',
                                        next_attempt_at = now() + interval '1 hour'""", Map.of());

        runner.scheduledRun();

        assertThat(freshness()).containsEntry("pending", 1L).containsEntry("has_oldest_pending", true)
            .containsEntry("dead", 0L);
        verify(heartbeat, never()).pollSucceeded();
    }
}
