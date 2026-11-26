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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * Republication: a marked submission's current records written again at the same version with new positions, and a
 * mark that stays while its republication fails. Marks are set here by SQL, as the admin's republication will set them.
 */
@FeedPublisherIntegrationTest
class RepublicationIT {

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
        // a reconcile pass requested at this context's start runs out first, whichever class comes first, so a batch
        // here does only this test's work
        while (publisher.runBatch()) {
            // the pass, and the retries of whatever it could not publish
        }
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

    private void mark(Ids submission) {
        fixture.execute("INSERT INTO feed_ops.republish (submission_id, marked_at) VALUES (:id, now())",
            Map.of("id", submission.id()));
    }

    private Map<String, Object> mark(String id) {
        List<Map<String, Object>> marks = jdbc.queryForList(
            "SELECT attempts, reason FROM feed_ops.republish WHERE submission_id = :id", Map.of("id", id));
        return marks.isEmpty() ? Map.of() : marks.get(0);
    }

    private Map<String, Map<String, Object>> rowsById(Ids submission) {
        return fixture.rows(submission).stream()
            .collect(Collectors.toMap(row -> (String) row.get("id"), Function.identity()));
    }

    @Test
    void aMarkedSubmissionIsWrittenAgainAtTheSameVersionWithNewPositions() {
        Ids submission = fixture.create(document("first", 2, row("r1", "a"), row("r2", "b", "p1:x")));
        drain();
        fixture.update(submission, document("first", 2, row("r1", "a")));
        drain();
        Map<String, Object> before = fixture.record(submission);
        Map<String, Map<String, Object>> rowsBefore = rowsById(submission);
        assertThat(rowsBefore.get("r2")).containsEntry("deleted", true);

        mark(submission);
        assertThat(publisher.runBatch()).isTrue();

        Map<String, Object> after = fixture.record(submission);
        assertThat(after).containsEntry("version", before.get("version"))
            .containsEntry("field_values", before.get("field_values"))
            .containsEntry("deleted", false);
        assertThat((Long) after.get("position")).isGreaterThan((Long) before.get("position"));
        Map<String, Map<String, Object>> rowsAfter = rowsById(submission);
        assertThat(rowsAfter).containsOnlyKeys("r1", "r2", "p1");
        rowsAfter.forEach((id, row) -> {
            Map<String, Object> earlier = rowsBefore.get(id);
            assertThat(row).as("row %s keeps its content", id)
                .containsEntry("deleted", earlier.get("deleted"))
                .containsEntry("changed_in_version", earlier.get("changed_in_version"))
                .containsEntry("field_values", earlier.get("field_values"));
            assertThat((Long) row.get("position")).as("row %s gets a new position", id)
                .isGreaterThan((Long) earlier.get("position"));
        });
        assertThat(mark(submission.id())).as("the mark is removed once written").isEmpty();
        assertThat(publisher.runBatch()).isFalse();
    }

    /**
     * A republication that fails keeps its mark (reconcile, which compares versions, would never find it: the feed
     * holds the head's version), and is written once the cause is fixed.
     */
    @Test
    void aFailedRepublicationKeepsItsMarkUntilItIsWritten() {
        fixture.fixPlacement();
        Ids submission = FeedFixture.nextIds();
        fixture.create(submission, document("placed", 1, row("r1", "a")), VERSION_2_UID, MISSING_ASSIGNMENT_UID);
        drain();
        Map<String, Object> published = fixture.record(submission);
        assertThat(published).containsEntry("version", fixture.head(submission));

        fixture.unfixPlacement();
        mark(submission);
        assertThat(publisher.runBatch()).isTrue();
        assertThat(mark(submission.id())).containsEntry("attempts", 1)
            .containsEntry("reason", FeedFailure.PLACEMENT_UNRESOLVED);
        assertThat(fixture.record(submission)).as("rolled back to its savepoint").isEqualTo(published);
        assertThat(fixture.pending(submission)).as("a republication is not a pending change").isEmpty();

        fixture.fixPlacement();
        assertThat(publisher.runBatch()).isTrue();
        assertThat(mark(submission.id())).isEmpty();
        assertThat((Long) fixture.record(submission).get("position")).isGreaterThan((Long) published.get("position"));
    }

    @Test
    void pendingRetriesComeBeforeRepublication() {
        Ids republished = fixture.create(document("republished", 1));
        drain();
        mark(republished);
        Ids retried = FeedFixture.nextIds();
        fixture.create(retried, document("retried", 1), VERSION_2_UID, MISSING_ASSIGNMENT_UID);
        drain();
        fixture.fixPlacement();

        assertThat(publisher.runBatch()).isTrue();
        assertThat(fixture.record(retried)).as("the retry, first").isNotEmpty();
        assertThat(mark(republished.id())).as("the mark waits for the next batch").isNotEmpty();
        assertThat(publisher.runBatch()).isTrue();
        assertThat(mark(republished.id())).isEmpty();
    }

    /**
     * An admin's republication of a form (feed build step F4): every record of the form is marked, read a page of the
     * records the feed holds at a time (three in these tests) and marked a page at a time, then written again at the
     * version the feed holds with new positions, behind the live backlog. A record of another form among them is not
     * marked, and a record whose held version is newer than its head is skipped, its mark gone.
     */
    @Test
    void anAdminsRepublicationOfAFormWritesItsRecordsAgainAndSkipsANewerHeldVersion() {
        List<Ids> ofForm = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ofForm.add(fixture.create(document("form " + i, i, row("r1", "a"))));
        }
        drain();
        List<String> created = ofForm.stream().map(Ids::id).toList();
        Ids otherForm = ofForm.remove(2);
        fixture.execute("UPDATE feed.submission SET form_id = 'fp-other-form' WHERE id = :id",
            Map.of("id", otherForm.id()));
        Ids newer = ofForm.get(ofForm.size() - 1);
        fixture.execute("UPDATE feed.submission SET version = version + 1 WHERE id = :id", Map.of("id", newer.id()));
        Map<String, Map<String, Object>> before = new HashMap<>();
        ofForm.forEach(submission -> before.put(submission.id(), fixture.record(submission)));
        Map<String, Object> otherBefore = fixture.record(otherForm);

        assertThat(publisher.markForRepublication(FORM_ID, null)).as("the form's five").isEqualTo(5);
        assertThat(mark(otherForm.id())).as("another form's record").isEmpty();
        assertThat(fixture.count("SELECT count(DISTINCT xmin::text) FROM feed_ops.republish", Map.of()))
            .as("pages of three: the form's two of the first, then three").isEqualTo(2);
        while (publisher.runBatch()) {
            // the marks are written
        }

        for (Ids submission : ofForm) {
            Map<String, Object> after = fixture.record(submission);
            if (submission == newer) {
                assertThat(after).as("a newer held version is skipped").isEqualTo(before.get(newer.id()));
                continue;
            }
            assertThat(after).as(submission.id()).containsEntry("version", before.get(submission.id()).get("version"))
                .containsEntry("field_values", before.get(submission.id()).get("field_values"));
            assertThat((Long) after.get("position")).isGreaterThan((Long) before.get(submission.id()).get("position"));
        }
        assertThat(fixture.record(otherForm)).as("not marked, not written").isEqualTo(otherBefore);
        assertThat(publisher.republishMarksWaiting()).isZero();
        assertThat(fixture.count("SELECT count(*) FROM outbox WHERE submission_id IN (:ids)", Map.of("ids", created)))
            .as("no outbox row written").isEqualTo(6);
    }

    /**
     * An admin's republication of every form marks each record the feed holds, and only those, three at a time in
     * these tests: each three in a short transaction of their own under the feed's lock, so live changes go on
     * between them, never one transaction for the whole scope. The marks are then written as before.
     */
    @Test
    void anAdminsRepublicationOfEveryFormMarksTheHeldRecordsAFewAtATimeEachInATransactionOfTheirOwn() {
        List<Ids> held = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            held.add(fixture.create(document("held " + i, i)));
        }
        drain();
        Ids lacked = fixture.create(document("lacked", 1));
        fixture.execute("UPDATE outbox SET status = 'success' WHERE status = 'pending'", Map.of());
        Map<String, Long> positions = new HashMap<>();
        held.forEach(submission -> positions.put(submission.id(), (Long) fixture.record(submission).get("position")));

        assertThat(publisher.markForRepublication(null, null)).isEqualTo(8);

        assertThat(publisher.republishMarksWaiting()).isEqualTo(8);
        assertThat(mark(lacked.id())).as("a submission the feed lacks is not marked").isEmpty();
        assertThat(fixture.count("SELECT count(DISTINCT xmin::text) FROM feed_ops.republish", Map.of()))
            .as("the transactions that wrote the marks: three, three and two").isEqualTo(3);
        while (publisher.runBatch()) {
            // the marks are written
        }
        assertThat(publisher.republishMarksWaiting()).isZero();
        for (Ids submission : held) {
            assertThat((Long) fixture.record(submission).get("position")).as("written again: %s", submission.id())
                .isGreaterThan(positions.get(submission.id()));
        }
        assertThat(fixture.record(lacked)).isEmpty();
    }

    /**
     * Listed submissions: only those are marked, and only records the feed holds, a batch of the list at a time (three
     * in these tests).
     */
    @Test
    void anAdminsRepublicationOfListedSubmissionsMarksOnlyTheRecordsTheFeedHolds() {
        List<Ids> listed = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            listed.add(fixture.create(document("listed " + i, i)));
        }
        Ids other = fixture.create(document("other", 1));
        drain();
        Map<String, Long> positions = new HashMap<>();
        listed.forEach(submission -> positions.put(submission.id(), (Long) fixture.record(submission).get("position")));
        Map<String, Object> otherBefore = fixture.record(other);
        List<String> ids = new ArrayList<>(listed.stream().map(Ids::id).toList());
        ids.add("01FDSUBNOTHELD0000000000");

        assertThat(publisher.markForRepublication(null, ids)).as("the four the feed holds").isEqualTo(4);
        assertThat(fixture.count("SELECT count(DISTINCT xmin::text) FROM feed_ops.republish", Map.of()))
            .as("batches of three: three, then one and the submission the feed lacks").isEqualTo(2);
        while (publisher.runBatch()) {
            // the marks are written
        }

        for (Ids submission : listed) {
            assertThat(mark(submission.id())).isEmpty();
            assertThat((Long) fixture.record(submission).get("position")).as("written again: %s", submission.id())
                .isGreaterThan(positions.get(submission.id()));
        }
        assertThat(fixture.record(other)).as("not listed").isEqualTo(otherBefore);
    }
}
