package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ACTIVITY_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ASSIGNMENT_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.COUNT_FIELD;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ITEM_FIELD;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.NAME_FIELD;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ORG_UNIT_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.PARTS_SECTION;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.PART_FIELD;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ROWS_SECTION;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.TEAM_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * The live path: the ETL publishes each change to the feed inside its row's transaction, after taking the feed's lock
 * first, in a savepoint that a failure rolls back alone. Changes are written the way the application writes them (the
 * submission, its revision and its outbox row in one transaction), then the application's ETL runs. Values are
 * invented.
 */
@FeedPublisherIntegrationTest
class SubmissionPublisherIT {

    @Autowired
    private EtlOrchestrator orchestrator;

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

    private final ObjectMapper json = new ObjectMapper();

    private FeedFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
    }

    @AfterEach
    void tearDown() {
        TestSplitPartners.PARTNERS.clear();
        fixture.clean();
    }

    /** Runs the application's ETL until no outbox row is due. */
    private void drain() {
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // every claimed row ends success, failed with a later retry, or dead
        }
    }

    private JsonNode parse(Object text) {
        try {
            return text == null ? null : json.readTree(text.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Map<String, Object>> rowsById(Ids ids) {
        return fixture.rows(ids).stream().collect(Collectors.toMap(row -> (String) row.get("id"), Function.identity()));
    }

    @Test
    void aChangeIsPublishedFromItsHeadWithItsRows() {
        Ids submission = fixture.create(document("first", 3, row("r1", "a", "p1:x"), row("r2", "b")));

        drain();

        Map<String, Object> record = fixture.record(submission);
        assertThat(record)
            .containsEntry("version", 0L)
            .containsEntry("deleted", false)
            .containsEntry("form_id", FORM_ID)
            .containsEntry("form_version_id", VERSION_2_ID)
            .containsEntry("assignment_id", ASSIGNMENT_ID)
            .containsEntry("team_id", TEAM_ID)
            .containsEntry("org_unit_id", ORG_UNIT_ID)
            .containsEntry("activity_id", ACTIVITY_ID)
            .containsEntry("unmapped_values", null);
        assertThat(parse(record.get("field_values")))
            .isEqualTo(parse("{\"" + NAME_FIELD + "\": \"first\", \"" + COUNT_FIELD + "\": 3}"));
        assertThat(record.get("created_at")).isNotNull();
        assertThat(record.get("entered_start_at")).isNotNull();
        assertThat(record.get("entered_end_at")).isNotNull();
        assertThat(jdbc.queryForObject("""
                SELECT f.changed_at = r.recorded_at AND f.created_at = s.created_date AT TIME ZONE 'UTC'
                       AND f.entered_start_at = s.start_entry_time AT TIME ZONE 'UTC'
                       AND f.entered_end_at = s.finished_entry_time AT TIME ZONE 'UTC'
                FROM feed.submission f
                JOIN data_submission s ON s.id = f.id
                JOIN journal.submission_revision r ON r.submission_id = f.id AND r.version = f.version
                WHERE f.id = :id""", Map.of("id", submission.id()), Boolean.class))
            .as("changed when the revision was recorded; created and entered as stored, in UTC").isTrue();

        Map<String, Map<String, Object>> rows = rowsById(submission);
        assertThat(rows).containsOnlyKeys("r1", "p1", "r2");
        assertRow(rows.get("r1"), null, ROWS_SECTION, 1, "{\"" + ITEM_FIELD + "\": \"a\"}");
        assertRow(rows.get("p1"), "r1", PARTS_SECTION, 1, "{\"" + PART_FIELD + "\": \"x\"}");
        assertRow(rows.get("r2"), null, ROWS_SECTION, 2, "{\"" + ITEM_FIELD + "\": \"b\"}");
        rows.values().forEach(row -> assertThat(row)
            .containsEntry("changed_in_version", 0L)
            .containsEntry("deleted", false)
            .containsEntry("id_origin", "STORED"));

        List<Long> positions = new java.util.ArrayList<>();
        positions.add((Long) record.get("position"));
        rows.values().forEach(row -> positions.add((Long) row.get("position")));
        assertThat(positions).doesNotHaveDuplicates();
        assertThat(jdbc.queryForObject("SELECT last_position FROM feed.freshness WHERE feed = 'submissions'",
            Map.of(), Long.class)).isEqualTo(positions.stream().mapToLong(Long::longValue).max().orElseThrow());
        assertThat(fixture.outboxStatuses(submission)).containsExactly("success");
        assertThat(fixture.pending(submission)).isEmpty();
    }

    private void assertRow(Map<String, Object> row, String parent, String section, int index, String values) {
        assertThat(row).containsEntry("parent_row_id", parent).containsEntry("section_id", section)
            .containsEntry("row_index", index).containsEntry("unmapped_values", null);
        assertThat(parse(row.get("field_values"))).isEqualTo(parse(values));
    }

    @Test
    void aLaterChangeRewritesTheRecordAndOnlyTheRowsThatChanged() {
        Ids submission = fixture.create(document("first", 3, row("r1", "a", "p1:x"), row("r2", "b")));
        drain();
        long recordBefore = (Long) fixture.record(submission).get("position");
        Map<String, Map<String, Object>> before = rowsById(submission);

        fixture.update(submission, document("second", 3, row("r1", "a2", "p1:x"), row("r3", "c")));
        drain();

        Map<String, Object> record = fixture.record(submission);
        assertThat(record).containsEntry("version", 1L);
        assertThat((Long) record.get("position")).isGreaterThan(recordBefore);
        assertThat(parse(record.get("field_values")).get(NAME_FIELD).asText()).isEqualTo("second");

        Map<String, Map<String, Object>> after = rowsById(submission);
        assertThat(after).containsOnlyKeys("r1", "p1", "r2", "r3");
        assertThat(after.get("r1")).as("a changed row: a new position, changed in this version")
            .containsEntry("changed_in_version", 1L).containsEntry("deleted", false);
        assertThat((Long) after.get("r1").get("position")).isGreaterThan((Long) before.get("r1").get("position"));
        assertThat(after.get("p1")).as("an unchanged row is not rewritten").isEqualTo(before.get("p1"));
        assertThat(after.get("r2")).as("a removed row: a deleted record, keeping its last values")
            .containsEntry("deleted", true).containsEntry("changed_in_version", 1L)
            .containsEntry("field_values", before.get("r2").get("field_values"));
        assertThat((Long) after.get("r2").get("position")).isGreaterThan((Long) before.get("r2").get("position"));
        assertThat(after.get("r3")).as("an added row").containsEntry("changed_in_version", 1L)
            .containsEntry("deleted", false).containsEntry("row_index", 2);
    }

    @Test
    void aDeletedSubmissionIsADeletedRecordWithEveryRowDeleted() {
        Ids submission = fixture.create(document("first", 3, row("r1", "a", "p1:x"), row("r2", "b")));
        drain();
        Map<String, Map<String, Object>> before = rowsById(submission);
        String valuesBefore = (String) fixture.record(submission).get("field_values");

        fixture.delete(submission);
        drain();

        assertThat(fixture.record(submission)).containsEntry("version", 1L).containsEntry("deleted", true)
            .containsEntry("field_values", valuesBefore);
        Map<String, Map<String, Object>> after = rowsById(submission);
        assertThat(after).containsOnlyKeys("r1", "p1", "r2");
        after.forEach((id, row) -> assertThat(row).as(id)
            .containsEntry("deleted", true)
            .containsEntry("changed_in_version", 1L)
            .containsEntry("field_values", before.get(id).get("field_values")));
    }

    /**
     * Outbox rows processed out of order: the newest change's row first, then the older ones, which find the head
     * already held and write nothing.
     */
    @Test
    void versionsNeverGoBackwards() {
        Ids submission = fixture.create(document("v0", 0));
        fixture.update(submission, document("v1", 1));
        fixture.update(submission, document("v2", 2));
        fixture.execute("""
            UPDATE outbox SET next_attempt_at = localtimestamp + interval '1 hour'
            WHERE submission_id = :id AND submission_version < 2""", Map.of("id", submission.id()));

        drain();
        Map<String, Object> newest = fixture.record(submission);
        assertThat(newest).containsEntry("version", 2L);

        fixture.execute("UPDATE outbox SET next_attempt_at = NULL WHERE submission_id = :id",
            Map.of("id", submission.id()));
        drain();

        assertThat(fixture.outboxStatuses(submission)).containsExactly("success", "success", "success");
        assertThat(fixture.record(submission)).as("the older changes wrote nothing").isEqualTo(newest);
    }

    @Test
    void aFeedFailureLeavesTallEventsAndTheRowsSuccessCommitted() {
        Ids submission = FeedFixture.nextIds();
        fixture.create(submission, document("unplaced", 1, row("r1", "a")), VERSION_2_UID, MISSING_ASSIGNMENT_UID);

        drain();

        assertThat(fixture.outboxStatuses(submission)).containsExactly("success");
        assertThat(fixture.count("SELECT count(*) FROM analytics.tall_canonical WHERE submission_uid = :uid",
            Map.of("uid", submission.uid()))).as("tall").isPositive();
        assertThat(fixture.count("SELECT count(*) FROM analytics.events WHERE submission_uid = :uid",
            Map.of("uid", submission.uid()))).as("events").isPositive();
        assertThat(fixture.record(submission)).isEmpty();
        assertThat(fixture.count("SELECT count(*) FROM feed.submission_row WHERE submission_id = :id",
            Map.of("id", submission.id()))).isZero();
        assertThat(fixture.pending(submission)).containsEntry("attempts", 1)
            .containsEntry("reason", FeedFailure.PLACEMENT_UNRESOLVED).containsEntry("dead_at", null);
    }

    /**
     * A feed part held up past its limit (another session holds the submission's record) is cancelled at three
     * seconds, well inside the row's ten: it rolls back to its savepoint, and the change's tall rows commit.
     */
    @Test
    void aFeedPartThatRunsOutOfTimeRollsBackAloneWithinTheRowsTimeout() throws Exception {
        Ids submission = fixture.create(document("first", 1, row("r1", "a")));
        drain();
        Map<String, Object> published = fixture.record(submission);
        fixture.update(submission, document("second", 1, row("r1", "a")));

        long took;
        try (Connection holder = fixture.connect()) {
            holder.setAutoCommit(false);
            try (PreparedStatement lock = holder.prepareStatement(
                "SELECT 1 FROM feed.submission WHERE id = ? FOR UPDATE")) {
                lock.setString(1, submission.id());
                lock.executeQuery().close();
            }
            long start = System.nanoTime();
            drain();
            took = System.nanoTime() - start;
            holder.rollback();
        }

        assertThat(Duration.ofNanos(took)).isBetween(Duration.ofSeconds(3), Duration.ofSeconds(9));
        assertThat(fixture.outboxStatuses(submission)).containsExactly("success", "success");
        assertThat(fixture.count("""
                SELECT count(*) FROM analytics.tall_canonical WHERE submission_uid = :uid AND value_text = 'second'""",
            Map.of("uid", submission.uid()))).as("the change's tall row").isEqualTo(1);
        assertThat(fixture.record(submission)).as("the feed still holds the first version").isEqualTo(published);
        assertThat(fixture.pending(submission)).containsEntry("attempts", 1)
            .containsEntry("reason", FeedFailure.TIMEOUT);
    }

    /**
     * The feed's lock is the row transaction's first statement: while another session holds it, the ETL's row waits
     * before writing tall and events, and goes on once it is released.
     */
    @Test
    void theFeedsLockIsTakenBeforeTallAndEvents() throws Exception {
        Ids submission = fixture.create(document("first", 1, row("r1", "a")));
        CompletableFuture<Void> etl;
        try (Connection holder = fixture.connect()) {
            holder.setAutoCommit(false);
            try (PreparedStatement lock = holder.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                lock.setLong(1, FeedLock.KEY);
                lock.executeQuery().close();
            }
            etl = CompletableFuture.runAsync(this::drain);
            assertThat(waitingOnTheFeedsLock(holder)).as("the row waits on the feed's lock").isTrue();
            assertThat(fixture.count("SELECT count(*) FROM analytics.tall_canonical WHERE submission_uid = :uid",
                Map.of("uid", submission.uid()))).as("no tall row yet").isZero();
            assertThat(fixture.count("SELECT count(*) FROM analytics.events WHERE submission_uid = :uid",
                Map.of("uid", submission.uid()))).as("no event yet").isZero();
            holder.rollback();
        }
        etl.get(10, TimeUnit.SECONDS);

        assertThat(fixture.record(submission)).containsEntry("version", 0L);
        assertThat(fixture.outboxStatuses(submission)).containsExactly("success");
    }

    private static boolean waitingOnTheFeedsLock(Connection watcher) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (PreparedStatement waiting = watcher.prepareStatement("""
            SELECT count(*) FROM pg_locks
            WHERE locktype = 'advisory' AND NOT granted
              AND ((classid::bigint << 32) | objid::bigint) = ?""")) {
            waiting.setLong(1, FeedLock.KEY);
            while (System.nanoTime() < deadline) {
                try (ResultSet count = waiting.executeQuery()) {
                    if (count.next() && count.getInt(1) > 0) {
                        return true;
                    }
                }
                Thread.sleep(20);
            }
        }
        return false;
    }

    /**
     * A restart in the middle of a publication: the row's connection is lost while its feed part runs, so nothing of
     * the row commits; the row is recorded for a retry, and the next run publishes it once.
     */
    @Test
    @org.junit.jupiter.api.Timeout(60)
    void aRowWhoseConnectionIsLostMidPublicationIsPublishedOnceByTheNextRun() throws Exception {
        Ids submission = fixture.create(document("first", 1, row("r1", "a")));
        fixture.execute("""
            CREATE OR REPLACE FUNCTION fd_test_hold() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
              PERFORM pg_sleep(30);
              RETURN NEW;
            END $$""", Map.of());
        fixture.execute("""
            CREATE TRIGGER fd_test_hold AFTER INSERT OR UPDATE ON feed.submission
            FOR EACH ROW EXECUTE FUNCTION fd_test_hold()""", Map.of());
        try {
            CompletableFuture<Void> etl = CompletableFuture.runAsync(this::drain);
            try (Connection watcher = fixture.connect()) {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                boolean terminated = false;
                while (!terminated && System.nanoTime() < deadline) {
                    try (PreparedStatement kill = watcher.prepareStatement("""
                        SELECT pg_terminate_backend(pid) FROM pg_stat_activity
                        WHERE datname = current_database() AND state = 'active' AND pid <> pg_backend_pid()
                          AND query ILIKE '%INSERT INTO feed.submission%'""");
                         ResultSet killed = kill.executeQuery()) {
                        terminated = killed.next() && killed.getBoolean(1);
                    }
                    Thread.sleep(50);
                }
                assertThat(terminated).as("the row's session was found in its feed part and ended").isTrue();
            }
            etl.get(30, TimeUnit.SECONDS);
        } finally {
            fixture.execute("DROP TRIGGER IF EXISTS fd_test_hold ON feed.submission", Map.of());
            fixture.execute("DROP FUNCTION IF EXISTS fd_test_hold()", Map.of());
        }
        assertThat(fixture.record(submission)).as("nothing of the row committed").isEmpty();
        assertThat(fixture.outboxRows(submission)).singleElement()
            .satisfies(row -> assertThat(row).containsEntry("status", "failed"));

        fixture.endOutboxWaits(submission);
        drain();

        assertThat(fixture.record(submission)).containsEntry("version", 0L);
        assertThat(fixture.rows(submission)).hasSize(1);
        assertThat(fixture.outboxStatuses(submission)).containsExactly("success");
        assertThat(fixture.count("""
                SELECT count(*) FROM (SELECT x.position
                                      FROM (SELECT position FROM feed.submission
                                            UNION ALL SELECT position FROM feed.submission_row) x
                                      GROUP BY x.position HAVING count(*) > 1) d""", Map.of())).isZero();
        assertThat(fixture.pending(submission)).isEmpty();
    }

    /** The split seam: a submission's partner is published with it, and the partner's own change is then skipped. */
    @Test
    void aSplitPartnersHeadIsPublishedInTheSameTransaction() {
        Ids source = fixture.create(document("source", 1, row("r1", "a")));
        Ids partner = fixture.create(document("partner", 1, row("r2", "b")));
        TestSplitPartners.PARTNERS.put(source.id(), List.of(partner.id()));
        fixture.execute("UPDATE outbox SET next_attempt_at = localtimestamp + interval '1 hour' WHERE submission_id = :id",
            Map.of("id", partner.id()));

        drain();

        assertThat(fixture.record(partner)).as("published with its partner").containsEntry("version", 0L);
        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT xmin::text) FROM feed.submission WHERE id IN (:ids)""",
            Map.of("ids", List.of(source.id(), partner.id())), Long.class))
            .as("both records written by one transaction").isEqualTo(1);
        Map<String, Object> published = fixture.record(partner);
        fixture.execute("UPDATE outbox SET next_attempt_at = NULL WHERE submission_id = :id",
            Map.of("id", partner.id()));
        drain();
        assertThat(fixture.outboxStatuses(partner)).containsExactly("success");
        assertThat(fixture.record(partner)).as("the partner's own change finds its head held").isEqualTo(published);
    }

    /**
     * Why the feed part's savepoint is taken on the connection: the application's transaction manager (JPA, with
     * Hibernate's dialect) cannot start a nested transaction.
     */
    @Test
    void theTransactionManagerCannotNestATransaction() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

        assertThatThrownBy(() -> outer.executeWithoutResult(status ->
            nested.executeWithoutResult(inner -> jdbc.queryForObject("SELECT 1", Map.of(), Integer.class))))
            .isInstanceOf(NestedTransactionNotSupportedException.class);
    }
}
