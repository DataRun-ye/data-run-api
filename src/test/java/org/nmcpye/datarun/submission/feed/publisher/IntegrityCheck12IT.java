package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.SqlTestContainer;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * Integrity check 12 on a feed the publisher wrote: its publication lines read 0, and each one counts the fault it is
 * for, broken by hand in the feed's tables. The database is shared with other tests, so each fault is measured as the
 * change it makes to the line. Its row count agrees with the submission normalizer's on documents of every shape the
 * walk tells apart. Values are invented.
 */
@FeedPublisherIntegrationTest
class IntegrityCheck12IT {

    private static final List<String> PUBLICATION_LINES = List.of("12a", "12b", "12c", "12d", "12e", "12f", "12g");

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private SubmissionPublisher publisher;

    @Autowired
    private SqlTestContainer database;

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

    /** Publishes every head in the database: the live changes, then a reconcile pass and the retries it leaves. */
    private void publishEverything() {
        drain();
        publisher.reconcileNightly();
        while (publisher.runBatch()) {
            // reconcile runs out, then the retries of what it could not publish, to their cap
        }
    }

    /** Documents of every shape check 12's walk tells apart from the normalizer's rows. */
    private List<Ids> submissionsOfEveryShape() {
        return List.of(
            fixture.create(document("rows and parts", 2, row("r1", "a", "p1:x", "p2:y"), row("r2", "b"))),
            fixture.create(document("no rows", 0)),
            fixture.create("""
                {"main": {"fp_name": "a field holding rows", "rows": [{"_id": "r1", "fp_item": "a"}]},
                 "elsewhere": {"list": [{"x": 1}, {"x": 2}]},
                 "main_rows_lookalike": [{"_id": "z"}],
                 "_status": "COMPLETE"}"""),
            fixture.create("""
                {"main": {"fp_name": [{"kept": "whole"}], "rows": [{"_id": "r1"}, {"_id": "r2", "fp_item": null}]}}"""),
            fixture.create("""
                {"main": {"rows": [{"_id": "r1", "parts": []}, "not a row"], "fp_count": 1}}"""),
            fixture.create("""
                {"main": {"rows": [{"_id": "r1", "parts": [{"_id": "p1", "fp_part": "x"}, {"fp_part": "derived"}]},
                                   {"_id": "r1", "fp_item": "same id"}]}}"""),
            fixture.create("[1, 2, 3]"),
            fixture.create("null"));
    }

    @Test
    void onAFeedThePublisherWroteEveryPublicationLineReadsZero() throws Exception {
        List<Ids> submissions = submissionsOfEveryShape();
        Ids deleted = fixture.create(document("deleted", 1, row("r1", "a")));
        fixture.delete(deleted);
        Ids changed = fixture.create(document("changed", 2, row("r1", "a"), row("r2", "b")));
        drain();
        fixture.update(changed, document("changed", 2, row("r1", "a2"), row("r3", "c")));

        publishEverything();
        IntegrityCheck12 check = IntegrityCheck12.run(database);

        for (Ids submission : submissions) {
            assertThat(fixture.record(submission)).as(submission.id()).isNotEmpty();
        }
        PUBLICATION_LINES.forEach(line -> assertThat(check.line(line)).as(line).isZero());
        assertThat(check.line("12h")).as("the feed's dead changes: its pending changes past the cap")
            .isEqualTo(fixture.count("SELECT count(*) FROM feed_ops.pending WHERE dead_at IS NOT NULL", Map.of()));
        assertThat(check.line("12i")).as("the backlog, for reference").isZero();
        assertThat(check.scoped().get("12j|PATH_NOT_IN_FORM")).as("unmapped values, for reference").isPositive();
        assertThat(check.line("12k")).isZero();
    }

    @Test
    void eachPublicationLineCountsTheFaultItIsFor() throws Exception {
        Ids first = fixture.create(document("first", 1, row("r1", "a"), row("r2", "b")));
        Ids second = fixture.create(document("second", 1, row("r1", "a")));
        Ids third = fixture.create(document("third", 1, row("r1", "a"), row("r2", "b")));
        publishEverything();
        IntegrityCheck12 before = IntegrityCheck12.run(database);
        PUBLICATION_LINES.forEach(line -> assertThat(before.line(line)).as(line).isZero());

        fixture.execute("DELETE FROM feed.submission_row WHERE submission_id = :id", Map.of("id", first.id()));
        fixture.execute("DELETE FROM feed.submission WHERE id = :id", Map.of("id", first.id()));
        fixture.execute("""
            INSERT INTO feed.submission (position, version, created_at, changed_at, published_at, deleted, id, form_id,
                                         assignment_id, team_id, org_unit_id, activity_id, field_values)
            VALUES (nextval('feed.position_seq'), 0, now(), now(), now(), false, 'no-such-submission', 'f', 'a', 't',
                    'o', 'c', '{}')""", Map.of());
        fixture.execute("""
            INSERT INTO feed.submission_row (position, changed_in_version, published_at, row_index, deleted,
                                             submission_id, id, id_origin, section_id, field_values)
            VALUES (nextval('feed.position_seq'), 0, now(), 1, false, 'no-such-record', 'r1', 'STORED', 's', '{}')""",
            Map.of());
        fixture.execute("UPDATE feed.submission SET deleted = true WHERE id = :id", Map.of("id", second.id()));
        fixture.execute("""
            UPDATE feed.submission_row SET deleted = true WHERE submission_id = :id AND id = 'r2'""",
            Map.of("id", third.id()));
        fixture.execute("""
            UPDATE feed.submission_row r SET position = f.position
            FROM feed.submission f WHERE f.id = r.submission_id AND r.submission_id = :id AND r.id = 'r1'""",
            Map.of("id", third.id()));
        fixture.execute("""
            INSERT INTO feed_ops.pending (submission_id, attempts, reason, first_failed_at, last_failed_at,
                                          next_attempt_at)
            VALUES ('waiting-too-long', 1, 'TIMEOUT', now() - interval '3 minutes', now(), now())""", Map.of());

        IntegrityCheck12 after = IntegrityCheck12.run(database);

        assertThat(after.line("12a") - before.line("12a")).as("12a: a head the feed lost").isEqualTo(1);
        assertThat(after.line("12b") - before.line("12b")).as("12b: a record without its submission").isEqualTo(1);
        assertThat(after.line("12c") - before.line("12c")).as("12c: a row record without its record").isEqualTo(1);
        assertThat(after.line("12d") - before.line("12d")).as("12d: the live row of a deleted record").isEqualTo(1);
        assertThat(after.line("12e") - before.line("12e")).as("12e: one position used twice").isEqualTo(1);
        assertThat(after.line("12f") - before.line("12f")).as("12f: a live row missing").isEqualTo(1);
        assertThat(after.scoped().getOrDefault("12f|" + FORM_UID, 0L)
            - before.scoped().getOrDefault("12f|" + FORM_UID, 0L)).as("12f: counted in its form's run").isEqualTo(1);
        assertThat(after.line("12g") - before.line("12g")).as("12g: a pending change past the target").isEqualTo(1);
    }

    @Test
    void deadChangesAreCountedByReasonCode() throws Exception {
        Ids unplaced = FeedFixture.nextIds();
        fixture.create(unplaced, document("unplaced", 1), VERSION_2_UID, MISSING_ASSIGNMENT_UID);
        Ids deadRow = fixture.create(document("dead outbox row", 1));
        fixture.execute("UPDATE outbox SET status = 'dead' WHERE submission_id = :id", Map.of("id", deadRow.id()));
        drain();
        IntegrityCheck12 before = IntegrityCheck12.run(database);
        while (publisher.runBatch()) {
            // the retries reach the cap
        }

        IntegrityCheck12 after = IntegrityCheck12.run(database);

        assertThat(fixture.pending(unplaced).get("dead_at")).isNotNull();
        assertThat(after.scoped().getOrDefault("12h|PLACEMENT_UNRESOLVED", 0L)
            - before.scoped().getOrDefault("12h|PLACEMENT_UNRESOLVED", 0L)).isEqualTo(1);
        assertThat(after.scoped()).as("an outbox row the runtime gave up on is not the feed's dead change")
            .doesNotContainKey("12h|OUTBOX_DEAD");
        assertThat(after.line("12i")).as("but its version, which the feed lacks, is in the backlog")
            .isGreaterThanOrEqualTo(1);
        assertThat(after.line("12g")).as("a dead change is no longer pending").isZero();
    }

    /**
     * 12f walks the heads one form at a time; a record whose submission names no form is in no form's run, and its
     * head has no rows, so it counts when it has a live row record.
     */
    @Test
    void aRecordWhoseSubmissionNamesNoFormCountsWhenItHasLiveRows() throws Exception {
        Ids withRows = fixture.create(document("with rows", 1, row("r1", "a")));
        Ids withoutRows = fixture.create(document("without rows", 0));
        publishEverything();
        IntegrityCheck12 before = IntegrityCheck12.run(database);

        fixture.execute("UPDATE data_submission SET template_uid = NULL WHERE id IN (:ids)",
            Map.of("ids", List.of(withRows.id(), withoutRows.id())));

        IntegrityCheck12 after = IntegrityCheck12.run(database);
        assertThat(after.line("12f") - before.line("12f")).isEqualTo(1);
        assertThat(after.scoped()).doesNotContainKey("12f|" + FORM_UID);
    }

    /** A mutation of the row records that the normalizer's count would see: an extra live row. */
    @Test
    void anExtraLiveRowIsCountedToo() throws Exception {
        Ids submission = fixture.create(document("one row", 1, row("r1", "a")));
        publishEverything();
        IntegrityCheck12 before = IntegrityCheck12.run(database);

        fixture.execute("""
            INSERT INTO feed.submission_row (position, changed_in_version, published_at, row_index, deleted,
                                             submission_id, id, id_origin, section_id, field_values)
            VALUES (nextval('feed.position_seq'), 0, now(), 2, false, :id, 'r9', 'STORED', 's', '{}')""",
            Map.of("id", submission.id()));

        assertThat(IntegrityCheck12.run(database).line("12f") - before.line("12f")).isEqualTo(1);
    }
}
