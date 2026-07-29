package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadAuthority;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadScope;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@IntegrationTest
class CaptureShadowBootstrapIntegrationTest {

    private static final String CAPTURE_ORG_ID = "cap-org-capture";
    private static final String CAPTURE_ORG_UID = "O9000000002";
    private static final String SUBMISSION_ID = "01JCAP00000000000000000001";
    private static final String SUBMISSION_UID = "D9000000001";
    private static final long SUBMISSION_SERIAL = 900_001;

    @Autowired
    private CaptureShadowBootstrap bootstrap;

    @Autowired
    private CaptureSourceSnapshot sourceSnapshot;

    @Autowired
    private CaptureBootstrapBatchTransaction batchTransaction;

    @Autowired
    private CaptureBootstrapFinalTransaction finalTransaction;

    @Autowired
    private org.nmcpye.datarun.assignmentshadow.bootstrap.AssignmentShadowBootstrap assignmentBootstrap;

    @Autowired
    private ReleasedWorkReadAuthority releasedWorkReadAuthority;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        cleanFixtures();
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void exactReplayPreservesActiveStateAndCompletedRerunWritesNothing() throws Exception {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            true,
            null,
            null,
            "2026-07-25T01:02:03.123456Z",
            "2026-07-25T08:09:10.654321Z"
        );
        insertSubmission(
            "01JCAP00000000000000000002",
            "D9000000002",
            900_002,
            false,
            "IN_PROGRESS",
            "{\"repeat\":[{\"value\":7}],\"name\":\"capture\"}",
            null,
            "2026-07-25T09:10:11.000001Z"
        );

        CaptureShadowBootstrapReport first = bootstrap.run();
        List<String> journalBeforeRerun = journalContent();
        CaptureShadowBootstrapReport second = bootstrap.run();

        assertThat(first.successful()).isTrue();
        assertThat(first.sourceBoundary().sourceCount()).isEqualTo(2);
        assertThat(first.identities())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(2, 0));
        assertThat(first.events())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(2, 0));
        assertThat(first.currentPointers())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(2, 0));
        assertThat(first.checkpoints())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(1, 0));
        assertThat(second.createdCount()).isZero();
        assertThat(second.identities())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 2));
        assertThat(second.events())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 2));
        assertThat(second.currentPointers())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 2));
        assertThat(second.checkpoints())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 1));
        assertThat(journalContent()).isEqualTo(journalBeforeRerun);

        JsonNode event = eventPayload(SUBMISSION_UID);
        JsonNode submission = event.path("submission");
        assertThat(submission.path("formData").isNull()).isTrue();
        assertThat(submission.path("status").isNull()).isTrue();
        assertThat(submission.path("deleted").booleanValue()).isTrue();
        assertThat(submission.path("deletedAt").textValue())
            .isEqualTo("2026-07-25T01:02:03.123456Z");
        assertThat(submission.path("formVersionUid").textValue())
            .isEqualTo("V9000000001");
        assertThat(submission.path("formVersionNumber").intValue()).isEqualTo(4);
        assertThat(submission.path("createdDate").textValue())
            .isEqualTo("2026-07-25T04:05:06.000001Z");
        assertThat(submission.path("lastModifiedDate").textValue())
            .isEqualTo("2026-07-25T08:09:10.654321Z");
        JsonNode objectSubmission = eventPayload("D9000000002").path("submission");
        assertThat(objectSubmission.path("formData").path("repeat").path(0).path("value").intValue())
            .isEqualTo(7);
        assertThat(objectSubmission.path("formData").path("name").textValue())
            .isEqualTo("capture");
        assertThat(objectSubmission.path("status").textValue()).isEqualTo("IN_PROGRESS");
        assertThat(objectSubmission.path("assignmentUid").textValue())
            .isEqualTo("S9000000001");
        assertThat(objectSubmission.path("teamUid").textValue()).isEqualTo("T9000000001");
        assertThat(objectSubmission.path("activityUid").textValue()).isEqualTo("A9000000001");
        assertThat(jdbc.queryForObject(
            "SELECT recorded_at FROM event_journal WHERE event_id = ?",
            Instant.class,
            CaptureShadowProtocol.captureEventId(SUBMISSION_UID)
        )).isEqualTo(Instant.parse("2026-07-25T08:09:10.654321Z"));
        assertThat(jdbc.queryForObject(
            """
                SELECT baseline_submission_id || '|' || baseline_submission_uid
                       || '|' || baseline_serial_number
                FROM capture_identity_link
                WHERE capture_id = ?
                """,
            String.class,
            CaptureShadowProtocol.captureId(SUBMISSION_UID)
        )).isEqualTo(SUBMISSION_ID + "|" + SUBMISSION_UID + "|" + SUBMISSION_SERIAL);
        assertThat(first.toOperatorText())
            .contains("source_rows=2")
            .contains("source_sha256=")
            .contains("current_pointers_created=2")
            .contains("current_pointers_existing=0")
            .contains("missing_identities=0")
            .contains("missing_current_pointers=0")
            .contains("difference_count=0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "\"text\"", "1"})
    void unsupportedSourceFormBodiesFailWithoutCaptureWrites(String formData) {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            formData,
            null,
            "2026-07-25T08:09:10.654321Z"
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessageContaining("form_data");
        assertThat(count("capture_identity_link")).isZero();
        assertThat(count("capture_current_projection")).isZero();
        assertThat(captureEventCount()).isZero();
    }

    @Test
    void unresolvedOrganizationUnitFailsWithoutFabricatedIdentity() {
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessageContaining("unresolved organization unit");
        assertThat(count("org_unit_identity_link")).isZero();
        assertThat(count("capture_identity_link")).isZero();
        assertThat(count("capture_current_projection")).isZero();
        assertThat(captureEventCount()).isZero();
    }

    @Test
    void interruptedCommittedBatchResumesWithoutDuplicateEvents() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertLargeSubmissionFixture(CaptureShadowBootstrap.BATCH_SIZE + 1);
        CaptureSourceBoundary boundary = sourceSnapshot.capture();

        CaptureBootstrapBatchResult interrupted = batchTransaction.persistNextBatch(
            boundary,
            CaptureShadowBootstrap.INITIAL_SERIAL_CURSOR
        );

        assertThat(interrupted.rows()).isEqualTo(CaptureShadowBootstrap.BATCH_SIZE);
        assertThat(count("capture_identity_link")).isEqualTo(CaptureShadowBootstrap.BATCH_SIZE);
        assertThat(count("capture_current_projection"))
            .isEqualTo(CaptureShadowBootstrap.BATCH_SIZE);
        assertThat(captureEventCount()).isEqualTo(CaptureShadowBootstrap.BATCH_SIZE);
        assertThat(checkpointCount()).isZero();

        CaptureShadowBootstrapReport resumed = bootstrap.run();

        assertThat(resumed.successful()).isTrue();
        assertThat(resumed.identities())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(1, 250));
        assertThat(resumed.events())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(1, 250));
        assertThat(resumed.currentPointers())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(1, 250));
        assertThat(count("capture_identity_link")).isEqualTo(251);
        assertThat(count("capture_current_projection")).isEqualTo(251);
        assertThat(captureEventCount()).isEqualTo(251);
        assertThat(checkpointCount()).isEqualTo(1);
    }

    @Test
    void deletingOnlyCurrentPointersReconstructsThemWithoutNewImmutableFacts() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        bootstrap.run();
        List<String> journalBeforeReplay = journalContent();
        String checkpointBeforeReplay = checkpointContent();

        jdbc.update("DELETE FROM capture_current_projection");
        CaptureShadowBootstrapReport replay = bootstrap.run();

        assertThat(replay.currentPointers())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(1, 0));
        assertThat(replay.identities())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 1));
        assertThat(replay.events())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 1));
        assertThat(replay.checkpoints())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 1));
        assertThat(replay.createdCount()).isEqualTo(1);
        assertThat(journalContent()).isEqualTo(journalBeforeReplay);
        assertThat(checkpointContent()).isEqualTo(checkpointBeforeReplay);
    }

    @Test
    void wrongPointerFailsFastAndRollsBackTheCompleteBatch() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertLargeSubmissionFixture(CaptureShadowBootstrap.BATCH_SIZE);
        bootstrap.run();
        jdbc.update("DELETE FROM capture_current_projection");
        UUID wrongEventId = UUID.fromString("83000000-0000-0000-0000-000000000001");
        insertUnrelatedTestEvent(wrongEventId);
        String lastSubmissionUid = "D0000000250";
        UUID lastCaptureId = CaptureShadowProtocol.captureId(lastSubmissionUid);
        jdbc.update(
            """
                INSERT INTO capture_current_projection (capture_id, source_event_id)
                VALUES (?, ?)
                """,
            lastCaptureId,
            wrongEventId
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessageContaining("capture_id=" + lastCaptureId)
            .hasMessageContaining(
                "expected_event_id=" + CaptureShadowProtocol.captureEventId(lastSubmissionUid)
            )
            .hasMessageContaining("actual_event_id=" + wrongEventId);
        assertThat(count("capture_current_projection")).isEqualTo(1);
        assertThat(count("capture_identity_link")).isEqualTo(CaptureShadowBootstrap.BATCH_SIZE);
        assertThat(captureEventCount()).isEqualTo(CaptureShadowBootstrap.BATCH_SIZE);
        assertThat(checkpointCount()).isEqualTo(1);
    }

    @Test
    void missingAndUnrelatedExtraPointersAreBothReportedAtEqualCardinality() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        CaptureShadowBootstrapReport completed = bootstrap.run();
        String checkpointBeforeMismatch = checkpointContent();
        jdbc.update("DELETE FROM capture_current_projection");
        UUID extraCaptureId = UUID.fromString("84000000-0000-0000-0000-000000000001");
        UUID extraEventId = UUID.fromString("84000000-0000-0000-0000-000000000002");
        jdbc.update(
            """
                INSERT INTO capture_identity_link (
                    capture_id, baseline_submission_uid,
                    baseline_submission_id, baseline_serial_number
                ) VALUES (?, 'D9999999999', '01JCAP99999999999999999999', 999999)
                """,
            extraCaptureId
        );
        insertUnrelatedTestEvent(extraEventId);
        jdbc.update(
            """
                INSERT INTO capture_current_projection (capture_id, source_event_id)
                VALUES (?, ?)
                """,
            extraCaptureId,
            extraEventId
        );
        CaptureShadowBootstrapReport.ItemCount zero =
            new CaptureShadowBootstrapReport.ItemCount(0, 0);

        CaptureShadowBootstrapReport mismatch = finalTransaction.compareAndCheckpoint(
            completed.sourceBoundary(),
            zero,
            zero,
            zero,
            zero
        );

        assertThat(count("capture_current_projection")).isEqualTo(1);
        assertThat(mismatch.missingCurrentPointerCount()).isEqualTo(1);
        assertThat(mismatch.currentPointerDifferenceCount()).isZero();
        assertThat(mismatch.extraCurrentPointerCount()).isEqualTo(1);
        assertThat(mismatch.differenceCount()).isEqualTo(3);
        assertThat(mismatch.diagnosticSamples()).hasSizeLessThanOrEqualTo(10);
        assertThat(mismatch.checkpoints())
            .isEqualTo(new CaptureShadowBootstrapReport.ItemCount(0, 1));
        assertThat(checkpointContent()).isEqualTo(checkpointBeforeMismatch);
        assertThat(checkpointCount()).isEqualTo(1);
    }

    @Test
    void finalComparisonReportsADifferentPointerWithoutReclassifyingTheEvent() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        CaptureShadowBootstrapReport completed = bootstrap.run();
        String checkpointBeforeMismatch = checkpointContent();
        UUID differentEventId = UUID.fromString("84500000-0000-0000-0000-000000000001");
        insertUnrelatedTestEvent(differentEventId);
        jdbc.update(
            """
                UPDATE capture_current_projection
                SET source_event_id = ?
                WHERE capture_id = ?
                """,
            differentEventId,
            CaptureShadowProtocol.captureId(SUBMISSION_UID)
        );
        CaptureShadowBootstrapReport.ItemCount zero =
            new CaptureShadowBootstrapReport.ItemCount(0, 0);

        CaptureShadowBootstrapReport mismatch = finalTransaction.compareAndCheckpoint(
            completed.sourceBoundary(),
            zero,
            zero,
            zero,
            zero
        );

        assertThat(mismatch.missingCurrentPointerCount()).isZero();
        assertThat(mismatch.currentPointerDifferenceCount()).isEqualTo(1);
        assertThat(mismatch.extraCurrentPointerCount()).isZero();
        assertThat(mismatch.eventDifferenceCount()).isZero();
        assertThat(mismatch.differenceCount()).isEqualTo(1);
        assertThat(checkpointContent()).isEqualTo(checkpointBeforeMismatch);
        assertThat(checkpointCount()).isEqualTo(1);
    }

    @Test
    void uniqueEventPointerConflictIsSanitizedAndCannotOverwriteCurrentState() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        bootstrap.run();
        jdbc.update("DELETE FROM capture_current_projection");
        UUID unrelatedCaptureId = UUID.fromString("85000000-0000-0000-0000-000000000001");
        jdbc.update(
            """
                INSERT INTO capture_identity_link (
                    capture_id, baseline_submission_uid,
                    baseline_submission_id, baseline_serial_number
                ) VALUES (?, 'D9999999999', '01JCAP99999999999999999999', 999999)
                """,
            unrelatedCaptureId
        );
        UUID expectedEventId = CaptureShadowProtocol.captureEventId(SUBMISSION_UID);
        jdbc.update(
            """
                INSERT INTO capture_current_projection (capture_id, source_event_id)
                VALUES (?, ?)
                """,
            unrelatedCaptureId,
            expectedEventId
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessage(
                "Capture current pointer insert conflicted for capture_id="
                    + CaptureShadowProtocol.captureId(SUBMISSION_UID)
            )
            .satisfies(exception ->
                assertThat(exception.getMessage())
                    .doesNotContain("uq_capture_current_source_event")
            );
        assertThat(jdbc.queryForObject(
            """
                SELECT source_event_id
                FROM capture_current_projection
                WHERE capture_id = ?
                """,
            UUID.class,
            unrelatedCaptureId
        )).isEqualTo(expectedEventId);
        assertThat(count("capture_current_projection")).isEqualTo(1);
    }

    @Test
    void sourceMovementIsReportedAndPreventsCheckpointCreation() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        CaptureSourceBoundary initial = sourceSnapshot.capture();
        CaptureBootstrapBatchResult batch = batchTransaction.persistNextBatch(
            initial,
            CaptureShadowBootstrap.INITIAL_SERIAL_CURSOR
        );
        jdbc.update(
            """
                UPDATE data_submission
                SET status = 'IN_PROGRESS',
                    last_modified_date = CAST(? AS timestamp)
                WHERE uid = ?
                """,
            "2026-07-25T08:09:11.654321",
            SUBMISSION_UID
        );

        CaptureShadowBootstrapReport report = finalTransaction.compareAndCheckpoint(
            initial,
            new CaptureShadowBootstrapReport.ItemCount(
                batch.orgUnitAliasesCreated(),
                batch.orgUnitAliasesExisting()
            ),
            new CaptureShadowBootstrapReport.ItemCount(
                batch.identitiesCreated(),
                batch.identitiesExisting()
            ),
            new CaptureShadowBootstrapReport.ItemCount(
                batch.eventsCreated(),
                batch.eventsExisting()
            ),
            new CaptureShadowBootstrapReport.ItemCount(
                batch.currentPointersCreated(),
                batch.currentPointersExisting()
            )
        );

        assertThat(report.successful()).isFalse();
        assertThat(report.sourceMovementCount()).isEqualTo(1);
        assertThat(report.eventDifferenceCount()).isEqualTo(1);
        assertThat(checkpointCount()).isZero();
    }

    @Test
    void extraBootstrapEventIsRejectedWithBoundedDiagnostics() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        bootstrap.run();
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'capture', 'baseline_submission_captured/v1', NULL,
                          'subject', ?, 'system:migration/datarun-baseline-capture',
                          now(), '{}'::jsonb)
                """,
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            UUID.fromString("20000000-0000-0000-0000-000000000001")
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapMismatchException.class)
            .satisfies(exception -> {
                CaptureShadowBootstrapReport report =
                    ((CaptureShadowBootstrapMismatchException) exception).report();
                assertThat(report.extraEventCount()).isEqualTo(1);
                assertThat(report.diagnosticSamples()).hasSizeLessThanOrEqualTo(10);
            });
    }

    @Test
    void unrelatedTestEventIsOutsideBootstrapExactSet() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        bootstrap.run();
        UUID laterEventId = UUID.fromString("10000000-0000-0000-0000-000000000002");
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'alert', 'test_only_unrelated_event/v1', NULL,
                          'process', ?, 'system:test', now(), '{}'::jsonb)
                """,
            laterEventId,
            UUID.fromString("20000000-0000-0000-0000-000000000002")
        );

        CaptureShadowBootstrapReport report = bootstrap.run();

        assertThat(report.successful()).isTrue();
        assertThat(report.createdCount()).isZero();
        assertThat(report.extraEventCount()).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM event_journal WHERE event_id = ?",
            Long.class,
            laterEventId
        )).isEqualTo(1);
    }

    @Test
    void conflictingOrgUnitAliasFailsWithoutPartialBatchWrites() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        jdbc.update(
            "INSERT INTO org_unit_identity_link (org_unit_id, baseline_org_unit_uid) VALUES (?, ?)",
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            CAPTURE_ORG_UID
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessageContaining("organization-unit alias");
        assertThat(count("capture_identity_link")).isZero();
        assertThat(count("capture_current_projection")).isZero();
        assertThat(captureEventCount()).isZero();
        assertThat(count("org_unit_identity_link")).isEqualTo(1);
    }

    @Test
    void conflictingEventEnvelopeRollsBackIdentityAndAliasWrites() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'capture', 'baseline_submission_captured/v1',
                          'A9000000001', 'subject', ?, 'wrong-actor',
                          CAST('2026-07-25 08:09:10.654321+00' AS timestamptz),
                          '{}'::jsonb)
                """,
            CaptureShadowProtocol.captureEventId(SUBMISSION_UID),
            UUID.fromString("40000000-0000-0000-0000-000000000001")
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessageContaining("capture event");
        assertThat(count("capture_identity_link")).isZero();
        assertThat(count("org_unit_identity_link")).isZero();
        assertThat(count("capture_current_projection")).isZero();
        assertThat(captureEventCount()).isEqualTo(1);
    }

    @Test
    void conflictingCheckpointIsReportedAndNeverOverwritten() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        jdbc.update(
            """
                INSERT INTO transition_checkpoint (
                    checkpoint_key, recorded_at, payload
                ) VALUES (?,
                          CAST('1970-01-01 00:00:00+00' AS timestamptz),
                          '{"sourceCount":999,"sourceMaxSerial":null,"sourceSha256":"wrong"}'::jsonb)
                """,
            CaptureShadowProtocol.CHECKPOINT_KEY
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(CaptureShadowBootstrapMismatchException.class)
            .satisfies(exception -> assertThat(
                ((CaptureShadowBootstrapMismatchException) exception)
                    .report()
                    .checkpointDifferenceCount()
            ).isEqualTo(1));
        assertThat(count("capture_identity_link")).isEqualTo(1);
        assertThat(captureEventCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT payload->>'sourceSha256' FROM transition_checkpoint WHERE checkpoint_key = ?",
            String.class,
            CaptureShadowProtocol.CHECKPOINT_KEY
        )).isEqualTo("wrong");
    }

    @Test
    void unrelatedCheckpointDoesNotSatisfyCaptureCheckpoint() throws Exception {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        CaptureSourceBoundary boundary = sourceSnapshot.capture();
        JsonNode payload = objectMapper.createObjectNode()
            .put("sourceCount", boundary.sourceCount())
            .put("sourceMaxSerial", boundary.sourceMaxSerial())
            .put("sourceSha256", boundary.sourceSha256());
        jdbc.update(
            """
                INSERT INTO transition_checkpoint (
                    checkpoint_key, recorded_at, payload
                ) VALUES (?, ?, CAST(? AS jsonb))
                """,
            "unrelated_transition/v1",
            Timestamp.from(boundary.maximumLastModifiedDate()),
            objectMapper.writeValueAsString(payload)
        );

        assertThat(bootstrap.run().successful()).isTrue();
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM transition_checkpoint WHERE checkpoint_key = ?",
            Long.class,
            CaptureShadowProtocol.CHECKPOINT_KEY
        )).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM transition_checkpoint WHERE checkpoint_key = ?",
            Long.class,
            "unrelated_transition/v1"
        )).isEqualTo(1);
    }

    @Test
    void advisoryLockRejectsConcurrentBootstrapCommand() throws Exception {
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_lock(?)");
            PreparedStatement unlock = connection.prepareStatement("SELECT pg_advisory_unlock(?)")
        ) {
            lock.setLong(1, CaptureShadowBootstrap.ADVISORY_LOCK_KEY);
            lock.execute();
            try {
                assertThatThrownBy(bootstrap::run)
                    .isInstanceOf(CaptureShadowBootstrapConflictException.class)
                    .hasMessageContaining("advisory lock");
            } finally {
                unlock.setLong(1, CaptureShadowBootstrap.ADVISORY_LOCK_KEY);
                unlock.execute();
            }
        }
    }

    @Test
    void schemaHasNoBaselineForeignKeysAndRetainsRequiredConstraints() {
        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        bootstrap.run();

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE capture_identity_link SET baseline_submission_uid = ? WHERE capture_id = ?",
            "D9999999999",
            CaptureShadowProtocol.captureId(SUBMISSION_UID)
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.update(
            "DELETE FROM capture_identity_link WHERE capture_id = ?",
            CaptureShadowProtocol.captureId(SUBMISSION_UID)
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
        assertThat(jdbc.queryForObject(
            """
                SELECT count(*)
                FROM information_schema.table_constraints
                WHERE table_schema = current_schema()
                  AND table_name = 'capture_identity_link'
                  AND constraint_type = 'FOREIGN KEY'
                """,
            Long.class
        )).isZero();
        assertThat(jdbc.queryForObject(
            """
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'capture_identity_link'
                  AND is_nullable = 'NO'
                """,
            Long.class
        )).isEqualTo(4);
        assertThat(jdbc.queryForList(
            """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = current_schema()
                  AND table_name = 'capture_identity_link'
                  AND constraint_type = 'UNIQUE'
                """,
            String.class
        )).containsExactlyInAnyOrder(
            "uq_capture_identity_submission_uid",
            "uq_capture_identity_submission_id",
            "uq_capture_identity_serial_number"
        );
        assertThat(jdbc.update(
            "DELETE FROM data_submission WHERE uid = ?",
            SUBMISSION_UID
        )).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM capture_identity_link WHERE baseline_submission_uid = ?",
            Long.class,
            SUBMISSION_UID
        )).isEqualTo(1);
        jdbc.update(
            """
                INSERT INTO capture_identity_link (
                    capture_id, baseline_submission_uid,
                    baseline_submission_id, baseline_serial_number
                ) VALUES (?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            "D9999999999",
            "01JCAP99999999999999999999",
            999_999L
        );
        assertThat(count("capture_identity_link")).isEqualTo(2);
        assertThatThrownBy(() -> jdbc.update(
            """
                INSERT INTO capture_identity_link (
                    capture_id, baseline_submission_uid,
                    baseline_submission_id, baseline_serial_number
                ) VALUES (?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            SUBMISSION_UID,
            SUBMISSION_ID,
            SUBMISSION_SERIAL
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void addedOrgUnitAliasDoesNotAlterAssignmentAuthorityOrReleasedReads() {
        insertAssignmentAuthorityFixture();
        assignmentBootstrap.run();
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn("U9000000001");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("T9000000001"));
        when(user.getFormAccess()).thenReturn(List.of());
        ReleasedWorkReadScope before = releasedWorkReadAuthority.readAll(user);
        long actorAliasesBefore = count("actor_identity_link");
        long accessRowsBefore = count("assignment_access_projection");

        insertOrgUnit(CAPTURE_ORG_ID, CAPTURE_ORG_UID);
        insertSubmission(
            SUBMISSION_ID,
            SUBMISSION_UID,
            SUBMISSION_SERIAL,
            false,
            null,
            "{}",
            null,
            "2026-07-25T08:09:10.654321Z"
        );
        bootstrap.run();
        ReleasedWorkReadScope after = releasedWorkReadAuthority.readAll(user);

        assertThat(after.assignmentUids()).isEqualTo(before.assignmentUids());
        assertThat(after.directOrgUnitUids()).isEqualTo(before.directOrgUnitUids());
        assertThat(after.formUids("S9000000001"))
            .isEqualTo(before.formUids("S9000000001"));
        assertThat(count("assignment_access_projection")).isEqualTo(accessRowsBefore);
        assertThat(count("actor_identity_link")).isEqualTo(actorAliasesBefore);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM org_unit_identity_link WHERE baseline_org_unit_uid = ?",
            Long.class,
            CAPTURE_ORG_UID
        )).isEqualTo(1);
    }

    @Test
    void commandIsDisabledByDefault() {
        assertThat(applicationContext.getBeansOfType(CaptureShadowBootstrapCommand.class)).isEmpty();
    }

    private void insertSubmission(
        String id,
        String uid,
        long serial,
        boolean deleted,
        String status,
        String formData,
        String deletedAt,
        String lastModified
    ) {
        jdbc.update(
            """
                INSERT INTO data_submission (
                    id, serial_number, uid, deleted, deleted_at, form_data, status,
                    template_uid, template_version_uid, template_version_no,
                    assignment_uid, team_uid, team_code, org_unit_uid,
                    org_unit_code, org_unit_name, activity_uid,
                    start_entry_time, finished_entry_time,
                    created_by, created_date, last_modified_by, last_modified_date
                ) VALUES (
                    ?, ?, ?, ?, CAST(? AS timestamp), CAST(? AS jsonb), ?,
                    'F9000000001', 'V9000000001', 4,
                    'S9000000001', 'T9000000001', 'TEAM', ?,
                    'OU', 'Capture org', 'A9000000001',
                    CAST('2026-07-25 02:03:04.000001' AS timestamp),
                    CAST('2026-07-25 03:04:05.999999' AS timestamp),
                    'system', CAST('2026-07-25 04:05:06.000001' AS timestamp),
                    NULL, CAST(? AS timestamp)
                )
                """,
            id,
            serial,
            uid,
            deleted,
            deletedAt == null ? null : deletedAt.replace("T", " ").replace("Z", ""),
            formData,
            status,
            CAPTURE_ORG_UID,
            lastModified.replace("T", " ").replace("Z", "")
        );
    }

    private void insertLargeSubmissionFixture(int count) {
        jdbc.update(
            """
                INSERT INTO data_submission (
                    id, serial_number, uid, deleted, form_data, status,
                    template_uid, template_version_uid, template_version_no,
                    assignment_uid, team_uid, org_unit_uid, activity_uid,
                    created_by, created_date, last_modified_date
                )
                SELECT
                    '01K' || lpad(value::text, 23, '0'),
                    910000 + value,
                    'D' || lpad(value::text, 10, '0'),
                    FALSE,
                    jsonb_build_object('value', value),
                    NULL,
                    'F9000000001', 'V9000000001', 4,
                    'S9000000001', 'T9000000001', ?, 'A9000000001',
                    'system',
                    CAST('2026-07-25 04:05:06.000001' AS timestamp),
                    CAST('2026-07-25 08:09:10.654321' AS timestamp)
                FROM generate_series(1, ?) value
                """,
            CAPTURE_ORG_UID,
            count
        );
    }

    private void insertAssignmentAuthorityFixture() {
        jdbc.update(
            """
                INSERT INTO project (id, uid, code, name, disabled, created_by)
                VALUES ('cap-project', 'P9000000001', 'cap-project', 'Capture project', FALSE, 'test')
                """
        );
        jdbc.update(
            """
                INSERT INTO activity (id, uid, code, name, disabled, project_id, created_by)
                VALUES ('cap-activity', 'A9000000001', 'cap-activity', 'Capture activity',
                        FALSE, 'cap-project', 'test')
                """
        );
        jdbc.update(
            """
                INSERT INTO app_user (
                    id, uid, mobile, login, password_hash, activated, created_by
                ) VALUES ('cap-user', 'U9000000001', '7000000099', 'capture-user', ?, TRUE, 'test')
                """,
            "x".repeat(60)
        );
        jdbc.update(
            """
                INSERT INTO team (
                    id, uid, code, name, disabled, activity_id, form_permissions, created_by
                ) VALUES ('cap-team', 'T9000000001', 'cap-team', 'Capture team', FALSE,
                          'cap-activity',
                          '[{"form":"F9000000001","permissions":["ADD_SUBMISSIONS"]}]'::jsonb,
                          'test')
                """
        );
        jdbc.update("INSERT INTO team_user (team_id, user_id) VALUES ('cap-team', 'cap-user')");
        insertOrgUnit("cap-org-assignment", "O9000000001");
        jdbc.update(
            """
                INSERT INTO assignment (
                    id, uid, deleted, activity_id, team_id, org_unit_id, forms, created_by
                ) VALUES ('cap-assignment', 'S9000000001', FALSE, 'cap-activity',
                          'cap-team', 'cap-org-assignment', '["F9000000001"]'::jsonb, 'test')
                """
        );
    }

    private void insertOrgUnit(String id, String uid) {
        jdbc.update(
            "INSERT INTO org_unit (id, uid, code, name, created_by) VALUES (?, ?, ?, ?, 'test')",
            id,
            uid,
            id,
            id
        );
    }

    private JsonNode eventPayload(String submissionUid) throws Exception {
        String payload = jdbc.queryForObject(
            """
                SELECT event.payload::text
                FROM capture_current_projection current_pointer
                JOIN capture_identity_link identity_link
                  ON identity_link.capture_id = current_pointer.capture_id
                JOIN event_journal event
                  ON event.event_id = current_pointer.source_event_id
                WHERE identity_link.baseline_submission_uid = ?
                """,
            String.class,
            submissionUid
        );
        return objectMapper.readTree(payload);
    }

    private void insertUnrelatedTestEvent(UUID eventId) {
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'alert', 'test_only_unrelated_event/v1', NULL,
                          'process', ?, 'system:test', now(), '{}'::jsonb)
                """,
            eventId,
            UUID.randomUUID()
        );
    }

    private List<String> journalContent() {
        return jdbc.queryForList(
            "SELECT event_id::text || '|' || payload::text FROM event_journal ORDER BY event_id",
            String.class
        );
    }

    private long captureEventCount() {
        Long value = jdbc.queryForObject(
            "SELECT count(*) FROM event_journal WHERE event_type = 'capture'",
            Long.class
        );
        return value == null ? 0 : value;
    }

    private long checkpointCount() {
        Long value = jdbc.queryForObject(
            "SELECT count(*) FROM transition_checkpoint WHERE checkpoint_key = ?",
            Long.class,
            CaptureShadowProtocol.CHECKPOINT_KEY
        );
        return value == null ? 0 : value;
    }

    private String checkpointContent() {
        return jdbc.queryForObject(
            """
                SELECT checkpoint_key || '|' || recorded_at::text || '|' || payload::text
                FROM transition_checkpoint
                WHERE checkpoint_key = ?
                """,
            String.class,
            CaptureShadowProtocol.CHECKPOINT_KEY
        );
    }

    private long count(String table) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }

    private void cleanFixtures() {
        jdbc.execute(
            """
                TRUNCATE TABLE
                    transition_checkpoint,
                    capture_current_projection,
                    capture_identity_link,
                    assignment_grant_projection,
                    assignment_identity_link,
                    assignment_role_definition,
                    org_unit_identity_link,
                    actor_identity_link,
                    event_journal
                RESTART IDENTITY CASCADE
                """
        );
        jdbc.update("DELETE FROM data_submission WHERE id LIKE '01JCAP%' OR id LIKE '01K%'");
        jdbc.update("DELETE FROM assignment WHERE id LIKE 'cap-%'");
        jdbc.update("DELETE FROM team_user WHERE team_id LIKE 'cap-%' OR user_id LIKE 'cap-%'");
        jdbc.update("DELETE FROM team WHERE id LIKE 'cap-%'");
        jdbc.update("DELETE FROM org_unit WHERE id LIKE 'cap-%'");
        jdbc.update("DELETE FROM activity WHERE id LIKE 'cap-%'");
        jdbc.update("DELETE FROM project WHERE id LIKE 'cap-%'");
        jdbc.update("DELETE FROM app_user WHERE id LIKE 'cap-%'");
    }
}
