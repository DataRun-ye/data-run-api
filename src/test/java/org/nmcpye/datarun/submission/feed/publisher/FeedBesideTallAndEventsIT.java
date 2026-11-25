package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * The feed never waits for tall and events: the runtime applies each output in a savepoint of its own, so a failure of
 * the legacy projection rolls back only its own writes, and the outbox row goes to its retry, while the feed's record
 * commits. The retry finds the head held and writes nothing to the feed. Values are invented.
 */
@FeedPublisherIntegrationTest
class FeedBesideTallAndEventsIT {

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
        fixture.clean();
    }

    private void drain() {
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // every claimed row ends success, failed with a later retry, or dead
        }
    }

    private long legacyRows(Ids submission) {
        return fixture.count("SELECT count(*) FROM analytics.tall_canonical WHERE submission_uid = :uid",
            Map.of("uid", submission.uid()))
            + fixture.count("SELECT count(*) FROM analytics.events WHERE submission_uid = :uid",
            Map.of("uid", submission.uid()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tall_canonical", "events"})
    void aFailingProjectionLeavesTheFeedPublishedAndTheRowInRetryWhichThenWritesNothingNewToTheFeed(String table) {
        Ids submission = fixture.create(document("first", 2, row("r1", "a"), row("r2", "b")));
        fixture.failLegacyWrites(table);

        drain();

        Map<String, Object> published = fixture.record(submission);
        assertThat(published).as("the feed holds the change").containsEntry("version", 0L);
        assertThat(fixture.rows(submission)).hasSize(2);
        assertThat(fixture.outboxRows(submission)).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("status", "failed").containsEntry("attempt", 1);
            assertThat((String) row.get("last_error")).contains("injected " + table + " failure");
        });
        assertThat(legacyRows(submission)).as("the projection's writes are rolled back").isZero();
        assertThat(fixture.pending(submission)).isEmpty();

        fixture.mendLegacyWrites();
        fixture.endOutboxWaits(submission);
        drain();

        assertThat(fixture.outboxStatuses(submission)).containsExactly("success");
        assertThat(fixture.count("SELECT count(*) FROM analytics.tall_canonical WHERE submission_uid = :uid",
            Map.of("uid", submission.uid()))).as("tall, on the retry").isPositive();
        assertThat(fixture.count("SELECT count(*) FROM analytics.events WHERE submission_uid = :uid",
            Map.of("uid", submission.uid()))).as("events, on the retry").isPositive();
        assertThat(fixture.record(submission)).as("the retry writes nothing new to the feed").isEqualTo(published);
    }

    /**
     * A projection that fails on every try: its outbox row is dead at the third attempt (in these tests), the feed has
     * held the change since the first, and reconcile has nothing to do for it.
     */
    @Test
    void aProjectionPastItsAttemptsLeavesADeadRowAndTheFeedCorrect() {
        Ids submission = fixture.create(document("first", 1, row("r1", "a")));
        fixture.failLegacyWrites("tall_canonical");
        drain();
        Map<String, Object> published = fixture.record(submission);
        for (int retry = 0; retry < 2; retry++) {
            fixture.endOutboxWaits(submission);
            drain();
        }
        assertThat(fixture.outboxRows(submission)).singleElement()
            .satisfies(row -> assertThat(row).containsEntry("status", "dead").containsEntry("attempt", 3));

        publisher.reconcileNightly();
        while (publisher.runBatch()) {
            // the pass runs out
        }

        assertThat(fixture.record(submission)).as("reconcile finds the head held: nothing to do").isEqualTo(published);
        assertThat(published).containsEntry("version", fixture.head(submission));
    }

    /**
     * A projection that waits on another session's lock until the row's time runs out leaves its transaction unable
     * to commit; the feed's record is then written again in a new transaction, with the row's retry, so it does not
     * wait for the retry either.
     */
    @Test
    @Timeout(60)
    void aProjectionThatRunsOutTheRowsTimeLeavesTheFeedPublishedInANewTransaction() throws Exception {
        Ids submission = fixture.create(document("first", 1, row("r1", "a")));
        drain();
        fixture.update(submission, document("second", 1, row("r1", "a")));

        try (Connection holder = fixture.connect()) {
            holder.setAutoCommit(false);
            try (PreparedStatement hold = holder.prepareStatement(
                "UPDATE analytics.tall_canonical SET updated_at = now() WHERE submission_uid = ?")) {
                hold.setString(1, submission.uid());
                assertThat(hold.executeUpdate()).isPositive();
            }
            drain();
            holder.rollback();
        }

        assertThat(fixture.record(submission)).as("the feed holds the second version").containsEntry("version", 1L);
        assertThat(fixture.outboxRows(submission)).last().satisfies(row -> {
            assertThat(row).containsEntry("status", "failed").containsEntry("attempt", 1);
            assertThat((String) row.get("last_error")).as("the projection's own failure").endsWith("[SQLState 57014]");
        });
        assertThat(fixture.pending(submission)).isEmpty();
    }
}
