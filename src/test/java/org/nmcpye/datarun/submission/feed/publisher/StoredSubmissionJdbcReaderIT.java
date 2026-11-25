package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.nmcpye.datarun.submission.normalized.jdbc.Placement;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader.Head;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader.HeadVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ACTIVITY_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ASSIGNMENT_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ORG_UNIT_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.TEAM_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * The stored-submission read seam (submission.normalized.jdbc) on submissions written the way the application writes
 * them: a head by id and in a batch, with its placement resolved to ids in the one place that resolves it, and a pass
 * over every submission's head version in pages. It lives with the feed's fixture, which writes such submissions.
 */
@FeedPublisherIntegrationTest
class StoredSubmissionJdbcReaderIT {

    @Autowired
    private StoredSubmissionJdbcReader reader;

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

    private <T> T read(Supplier<T> reading) {
        return new TransactionTemplate(transactionManager).execute(status -> reading.get());
    }

    @Test
    void aHeadIsTheNewestRevisionWithTheStoredColumnsItLeavesOutAndItsPlacementAsIds() {
        Ids submission = fixture.create(document("first", 1, row("r1", "a")));
        fixture.update(submission, document("second", 2, row("r1", "b")));

        Head head = read(() -> reader.head(submission.id())).orElseThrow();

        assertThat(head.version()).isEqualTo(1);
        assertThat(head.deleted()).isFalse();
        assertThat(head.submission().id()).isEqualTo(submission.id());
        assertThat(head.submission().formUid()).isEqualTo(FORM_UID);
        assertThat(head.submission().formVersionUid()).isEqualTo(VERSION_2_UID);
        assertThat(head.submission().formData()).contains("second").doesNotContain("first");
        assertThat(head.formId()).isEqualTo(FORM_ID);
        assertThat(head.formVersionId()).isEqualTo(VERSION_2_ID);
        assertThat(head.placement()).isEqualTo(new Placement(ASSIGNMENT_ID, TEAM_ID, ORG_UNIT_ID, ACTIVITY_ID));
        assertThat(head.placement().complete()).isTrue();
        assertThat(head.createdAt()).isNotNull();
        assertThat(head.enteredStartAt()).isBefore(head.enteredEndAt());
        assertThat(head.changedAt()).isNotNull();

        fixture.delete(submission);
        assertThat(read(() -> reader.head(submission.id())).orElseThrow())
            .satisfies(deleted -> assertThat(deleted.version()).isEqualTo(2))
            .satisfies(deleted -> assertThat(deleted.deleted()).isTrue());
    }

    @Test
    void aPlacementUidThatNamesNothingLeavesItsIdMissing() {
        Ids unplaced = FeedFixture.nextIds();
        fixture.create(unplaced, document("unplaced", 1), VERSION_2_UID, MISSING_ASSIGNMENT_UID);

        Placement placement = read(() -> reader.head(unplaced.id())).orElseThrow().placement();

        assertThat(placement.assignmentId()).isNull();
        assertThat(placement.teamId()).isEqualTo(TEAM_ID);
        assertThat(placement.complete()).isFalse();
    }

    @Test
    void headsAreReadInBatchesAndASubmissionWithoutARevisionHasNone() {
        Ids first = fixture.create(document("first", 1));
        Ids second = fixture.create(document("second", 1));
        Ids withoutRevision = FeedFixture.nextIds();
        fixture.execute("""
            INSERT INTO data_submission (id, uid, template_uid, template_version_uid, assignment_uid, activity_uid,
                                         team_uid, org_unit_uid, form_data, lock_version, created_by, created_date,
                                         deleted)
            VALUES (:id, :uid, 'fpForm00001', 'fpFormVer02', 'fpAssign001', 'fpActivity1', 'fpTeam00001',
                    'fpOrgUnit01', '{}'::jsonb, 0, 'fp-test', timezone('UTC', now()), false)""",
            Map.of("id", withoutRevision.id(), "uid", withoutRevision.uid()));

        List<Head> heads = read(() -> reader.heads(List.of(first.id(), second.id(), withoutRevision.id())));
        assertThat(heads).extracting(head -> head.submission().id())
            .containsExactlyInAnyOrder(first.id(), second.id());
        assertThat(read(() -> reader.head(withoutRevision.id()))).isEmpty();

        List<HeadVersion> page = read(() -> reader.headVersionsAfter(FeedFixture.ID_PREFIX, 1000)).stream()
            .filter(head -> head.submissionId().startsWith(FeedFixture.ID_PREFIX)).toList();
        assertThat(page).extracting(HeadVersion::submissionId)
            .containsExactly(first.id(), second.id(), withoutRevision.id());
        assertThat(page).extracting(HeadVersion::version).containsExactly(0L, 0L, null);
        assertThat(page).extracting(HeadVersion::formUid).containsOnly(FORM_UID);
        assertThat(read(() -> reader.headVersionsAfter(first.id(), 1))).extracting(HeadVersion::submissionId)
            .containsExactly(second.id());
    }
}
