package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * Pending changes: retried behind the live backlog, each failure counted, dead at the cap (three attempts in these
 * tests, retries due at once), and cleared by a later publication of the submission's head, one that read the head
 * after the pending change was recorded. The failure is a real
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

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

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
        assertThat(publisher.runBatch()).isTrue();

        assertThat(fixture.record(submission)).containsEntry("version", 0L);
        assertThat(fixture.rows(submission)).hasSize(1);
        assertThat(fixture.pending(submission)).isEmpty();
        assertThat(publisher.runBatch()).as("nothing left to retry").isFalse();
    }

    @Test
    void aFailureThatRecursOnEveryRetryReachesTheCapAndGoesDead() {
        Ids submission = unplaced();
        drain();
        assertThat(fixture.pending(submission)).containsEntry("attempts", 1).containsEntry("dead_at", null);

        assertThat(publisher.runBatch()).isTrue();
        assertThat(fixture.pending(submission)).containsEntry("attempts", 2).containsEntry("dead_at", null);

        assertThat(publisher.runBatch()).isTrue();
        Map<String, Object> dead = fixture.pending(submission);
        assertThat(dead).containsEntry("attempts", 3).containsEntry("reason", FeedFailure.PLACEMENT_UNRESOLVED);
        assertThat(dead.get("dead_at")).as("dead at the cap").isNotNull();

        assertThat(publisher.runBatch()).as("a dead change is not retried").isFalse();
        assertThat(fixture.pending(submission)).isEqualTo(dead);
        assertThat(fixture.record(submission)).isEmpty();
    }

    @Test
    void aLaterLiveChangeOfADeadSubmissionPublishesItAndClearsTheDeadChange() {
        Ids submission = unplaced();
        drain();
        publisher.runBatch();
        publisher.runBatch();
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

    /**
     * A change whose feed part could not take the lock records its pending change without it, so one can arrive while
     * a publication of the same submission runs. Here it is recorded, from another session, after the publication read
     * the head and before it writes: the publication removes only what it saw before it read the head, so the newer
     * pending change stays for its retry.
     */
    @Test
    void aPendingChangeRecordedAfterAPublicationReadTheHeadStaysForItsRetry() {
        Ids submission = fixture.create(document("first", 1, row("r1", "a")));
        AtomicBoolean recorded = new AtomicBoolean();
        DataSource beforeTheWrite = new DelegatingDataSource(dataSource) {
            @Override
            public Connection getConnection() throws SQLException {
                return recordingBeforeTheWrite(super.getConnection(), () -> {
                    if (recorded.compareAndSet(false, true)) {
                        recordPendingElsewhere(submission);
                    }
                });
            }
        };
        FeedWriter writer = new FeedWriter(new FormDefinitions(), objectMapper, SplitPartners.NONE);

        new TransactionTemplate(new DataSourceTransactionManager(beforeTheWrite)).executeWithoutResult(status -> {
            FeedLock.take(new JdbcTemplate(beforeTheWrite));
            writer.publish(new FeedStatements(beforeTheWrite, System.nanoTime() + TimeUnit.SECONDS.toNanos(5)),
                submission.id(), false);
        });

        assertThat(recorded).isTrue();
        assertThat(fixture.record(submission)).containsEntry("version", 0L);
        assertThat(fixture.pending(submission)).as("recorded after the head was read: it stays")
            .containsEntry("reason", "TIMEOUT");
    }

    /** The connection, running the action just before it prepares the statement that writes the feed's records. */
    private static Connection recordingBeforeTheWrite(Connection target, Runnable action) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if (method.getName().equals("prepareStatement") && args != null && args[0] instanceof String sql
                    && sql.contains("written_submission")) {
                    action.run();
                }
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
    }

    /** A pending change for the submission, committed from a session of its own, as a feed part without the lock. */
    private void recordPendingElsewhere(Ids submission) {
        try (Connection other = fixture.connect(); PreparedStatement insert = other.prepareStatement("""
            INSERT INTO feed_ops.pending (submission_id, attempts, reason, first_failed_at, last_failed_at,
                                          next_attempt_at)
            VALUES (?, 1, 'TIMEOUT', now(), now(), now())""")) {
            insert.setString(1, submission.id());
            insert.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
