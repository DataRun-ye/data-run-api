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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
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
}
