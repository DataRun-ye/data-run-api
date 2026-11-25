package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * Pending changes: retried behind the live backlog, each failure counted, dead at the cap (three attempts in these
 * tests, retries due at once), and cleared by any later publication of the submission's head. The failure is a real
 * one, not injected: an assignment uid that names no assignment, which a test can fix by storing the assignment.
 */
@FeedPublisherIntegrationTest
class PendingRetriesIT {

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private SubmissionPublisher publisher;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    private Ids unplaced() {
        Ids submission = FeedFixture.nextIds();
        fixture.create(submission, document("unplaced", 1, row("r1", "a")), VERSION_2_UID, MISSING_ASSIGNMENT_UID);
        return submission;
    }

    @Test
    void aPendingChangeIsRetriedAndPublishedOnceItsCauseIsFixed() {
        Ids submission = unplaced();
        drain();
        assertThat(fixture.pending(submission)).containsEntry("attempts", 1);

        fixture.fixPlacement();
        assertThat(publisher.publishAfterBacklog()).isTrue();

        assertThat(fixture.record(submission)).containsEntry("version", 0L);
        assertThat(fixture.rows(submission)).hasSize(1);
        assertThat(fixture.pending(submission)).isEmpty();
        assertThat(publisher.publishAfterBacklog()).as("nothing left to retry").isFalse();
    }

    @Test
    void aFailureThatRecursOnEveryRetryReachesTheCapAndGoesDead() {
        Ids submission = unplaced();
        drain();
        assertThat(fixture.pending(submission)).containsEntry("attempts", 1).containsEntry("dead_at", null);

        assertThat(publisher.publishAfterBacklog()).isTrue();
        assertThat(fixture.pending(submission)).containsEntry("attempts", 2).containsEntry("dead_at", null);

        assertThat(publisher.publishAfterBacklog()).isTrue();
        Map<String, Object> dead = fixture.pending(submission);
        assertThat(dead).containsEntry("attempts", 3).containsEntry("reason", FeedFailure.PLACEMENT_UNRESOLVED);
        assertThat(dead.get("dead_at")).as("dead at the cap").isNotNull();

        assertThat(publisher.publishAfterBacklog()).as("a dead change is not retried").isFalse();
        assertThat(fixture.pending(submission)).isEqualTo(dead);
        assertThat(fixture.record(submission)).isEmpty();
    }

    @Test
    void aLaterLiveChangeOfADeadSubmissionPublishesItAndClearsTheDeadChange() {
        Ids submission = unplaced();
        drain();
        publisher.publishAfterBacklog();
        publisher.publishAfterBacklog();
        assertThat(fixture.pending(submission).get("dead_at")).isNotNull();

        fixture.fixPlacement();
        fixture.update(submission, document("changed", 1, row("r1", "a")));
        drain();

        assertThat(fixture.record(submission)).containsEntry("version", 1L);
        assertThat(fixture.pending(submission)).isEmpty();
    }

    /** The wait after each failure doubles from the retry delay up to the longest wait; the cap makes it dead. */
    @Test
    void eachFailureWaitsTwiceAsLongUpToTheLongestWait() {
        FeedPublisherProperties properties = new FeedPublisherProperties();
        properties.setRetryDelay(Duration.ofMinutes(1));
        properties.setMaxRetryDelay(Duration.ofMinutes(5));
        properties.setMaxAttempts(5);
        PendingChanges pending = new PendingChanges(properties);
        Ids submission = FeedFixture.nextIds();

        List<Long> waits = new ArrayList<>();
        List<Boolean> dead = new ArrayList<>();
        for (int failure = 1; failure <= 6; failure++) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                dead.add(pending.recordFailure(jdbc, submission.id(), FeedFailure.TIMEOUT).dead());
                waits.add(jdbc.queryForObject("""
                        SELECT extract(epoch FROM next_attempt_at - last_failed_at)::bigint
                        FROM feed_ops.pending WHERE submission_id = :id""",
                    Map.of("id", submission.id()), Long.class));
            });
        }

        assertThat(waits).containsExactly(60L, 120L, 240L, 300L, 300L, 300L);
        assertThat(dead).containsExactly(false, false, false, false, true, true);
        assertThat(fixture.pending(submission)).containsEntry("attempts", 6);
    }
}
