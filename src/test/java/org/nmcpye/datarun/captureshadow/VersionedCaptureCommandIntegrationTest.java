package org.nmcpye.datarun.captureshadow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.assignmentshadow.TransitionIdentityResolver;
import org.nmcpye.datarun.assignmentshadow.VersionedUploadAuthorityReceipt;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.config.datarun.DatarunProperties;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class VersionedCaptureCommandIntegrationTest {

    private static final String USER_UID = "U9300000001";
    private static final String SUBMISSION_UID = "D9300000001";
    private static final String ASSIGNMENT_UID = "S9300000001";
    private static final String ORG_UNIT_UID = "O9300000001";
    private static final UUID ASSIGNMENT_ID =
        UUID.fromString("93000000-0000-0000-0000-000000000001");
    private static final UUID GRANT_EVENT_ID =
        UUID.fromString("93000000-0000-0000-0000-000000000002");

    @Autowired
    private VersionedCaptureCommand command;

    @Autowired
    private DatarunProperties properties;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cleanFixtures();
    }

    @AfterEach
    void tearDown() {
        properties.getTransition().setCaptureLiveShadowEnabled(false);
        cleanFixtures();
    }

    @Test
    void disabledShadowPreservesBaselineAndWritesNoTransitionState() {
        properties.getTransition().setCaptureLiveShadowEnabled(false);

        execute(submission(SUBMISSION_UID, 1, false), administrator());

        assertThat(submissionValue(SUBMISSION_UID)).isEqualTo(1);
        assertThat(outboxCount(SUBMISSION_UID)).isEqualTo(1);
        assertThat(captureEventCount()).isZero();
        assertThat(count("capture_identity_link")).isZero();
        assertThat(count("capture_current_projection")).isZero();
        assertThat(count("actor_identity_link")).isZero();
        assertThat(count("org_unit_identity_link")).isZero();
    }

    @Test
    void createUpdateDeleteAndRetriesProduceOneExactChain() throws Exception {
        properties.getTransition().setCaptureLiveShadowEnabled(true);

        execute(submission(SUBMISSION_UID, 1, false), administrator());
        UUID createdEvent = currentEvent(SUBMISSION_UID);
        execute(submission(SUBMISSION_UID, 1, false), administrator());

        assertThat(captureEventCount()).isEqualTo(1);
        assertThat(outboxCount(SUBMISSION_UID)).isEqualTo(1);
        assertThat(currentEvent(SUBMISSION_UID)).isEqualTo(createdEvent);

        execute(submission(SUBMISSION_UID, 2, false), administrator());
        UUID updatedEvent = currentEvent(SUBMISSION_UID);
        execute(submission(SUBMISSION_UID, 2, true), administrator());
        UUID deletedEvent = currentEvent(SUBMISSION_UID);
        execute(submission(SUBMISSION_UID, 2, true), administrator());

        assertThat(captureEventCount()).isEqualTo(3);
        assertThat(outboxCount(SUBMISSION_UID)).isEqualTo(3);
        assertThat(currentEvent(SUBMISSION_UID)).isEqualTo(deletedEvent);
        assertThat(jdbc.queryForObject(
            "SELECT deleted FROM data_submission WHERE uid = ?",
            Boolean.class,
            SUBMISSION_UID
        )).isTrue();
        List<JsonNode> payloads = capturePayloads(SUBMISSION_UID);
        assertThat(payloads).hasSize(3);
        assertThat(payloads.get(0).path("previousEventId").isNull()).isTrue();
        assertThat(payloads.get(1).path("previousEventId").textValue())
            .isEqualTo(createdEvent.toString());
        assertThat(payloads.get(2).path("previousEventId").textValue())
            .isEqualTo(updatedEvent.toString());
        assertThat(payloads.get(2).path("submission").path("deleted").booleanValue())
            .isTrue();
    }

    @Test
    void bootstrapGrantReceiptIsRecordedWithoutCopiedGrantState() throws Exception {
        properties.getTransition().setCaptureLiveShadowEnabled(true);
        execute(submission(SUBMISSION_UID, 1, false), administrator());
        insertBootstrapGrant();

        execute(
            submission(SUBMISSION_UID, 2, false),
            VersionedUploadAuthorityReceipt.assignment(
                USER_UID,
                TransitionIdentityResolver.actorIdFor(USER_UID),
                GRANT_EVENT_ID
            )
        );

        JsonNode acceptance = capturePayloads(SUBMISSION_UID)
            .get(1)
            .path("acceptance");
        assertThat(acceptance.fieldNames())
            .toIterable()
            .containsExactlyInAnyOrder("kind", "grantEventId");
        assertThat(acceptance.path("kind").textValue()).isEqualTo("assignment");
        assertThat(acceptance.path("grantEventId").textValue())
            .isEqualTo(GRANT_EVENT_ID.toString());
    }

    @Test
    void shadowValidationFailureRollsBackBaselineAndOutbox() {
        properties.getTransition().setCaptureLiveShadowEnabled(true);
        execute(submission(SUBMISSION_UID, 1, false), administrator());
        UUID pointerBefore = currentEvent(SUBMISSION_UID);
        long outboxBefore = outboxCount(SUBMISSION_UID);
        long eventsBefore = captureEventCount();

        assertThatThrownBy(() -> execute(
            submission(SUBMISSION_UID, 2, false),
            VersionedUploadAuthorityReceipt.assignment(
                USER_UID,
                TransitionIdentityResolver.actorIdFor(USER_UID),
                UUID.fromString("93000000-0000-0000-0000-000000000099")
            )
        )).isInstanceOf(CaptureShadowConflictException.class)
            .hasMessageContaining("grant event does not exist");

        assertThat(submissionValue(SUBMISSION_UID)).isEqualTo(1);
        assertThat(outboxCount(SUBMISSION_UID)).isEqualTo(outboxBefore);
        assertThat(captureEventCount()).isEqualTo(eventsBefore);
        assertThat(currentEvent(SUBMISSION_UID)).isEqualTo(pointerBefore);
    }

    @Test
    void missingPointerRollsBackBaselineAndOutbox() {
        properties.getTransition().setCaptureLiveShadowEnabled(true);
        execute(submission(SUBMISSION_UID, 1, false), administrator());
        long outboxBefore = outboxCount(SUBMISSION_UID);
        long eventsBefore = captureEventCount();
        jdbc.update(
            "DELETE FROM capture_current_projection WHERE capture_id = ?",
            CaptureShadowProtocol.captureId(SUBMISSION_UID)
        );

        assertThatThrownBy(() -> execute(
            submission(SUBMISSION_UID, 2, false),
            administrator()
        )).isInstanceOf(CaptureShadowConflictException.class)
            .hasMessageContaining("Missing capture current pointer");

        assertThat(submissionValue(SUBMISSION_UID)).isEqualTo(1);
        assertThat(outboxCount(SUBMISSION_UID)).isEqualTo(outboxBefore);
        assertThat(captureEventCount()).isEqualTo(eventsBefore);
        assertThat(count("capture_current_projection")).isZero();
    }

    private VersionedUploadAuthorityReceipt administrator() {
        return VersionedUploadAuthorityReceipt.administrator(USER_UID);
    }

    private void execute(
        DataSubmission submission,
        VersionedUploadAuthorityReceipt authority
    ) {
        command.execute(
            List.of(new VersionedCaptureSubmissionCommand(submission, authority)),
            new EntitySaveSummaryVM()
        );
    }

    private DataSubmission submission(String uid, int value, boolean deleted) {
        DataSubmission submission = new DataSubmission();
        submission.setUid(uid);
        submission.setDeleted(deleted);
        submission.setFormData(objectMapper.createObjectNode().put("value", value));
        submission.setForm("F9300000001");
        submission.setFormVersion("V9300000001");
        submission.setVersion(1);
        submission.setAssignment(ASSIGNMENT_UID);
        submission.setTeam("T9300000001");
        submission.setTeamCode("TEAM");
        submission.setOrgUnit(ORG_UNIT_UID);
        submission.setOrgUnitCode("OU");
        submission.setOrgUnitName("Capture org");
        submission.setActivity("A9300000001");
        submission.setStartEntryTime(Instant.parse("2026-07-28T01:00:00Z"));
        return submission;
    }

    private void insertBootstrapGrant() {
        UUID actorId = TransitionIdentityResolver.actorIdFor(USER_UID);
        UUID orgUnitId = TransitionIdentityResolver.orgUnitIdFor(ORG_UNIT_UID);
        jdbc.update(
            "INSERT INTO assignment_role_definition (role_key, activity_uid, form_uids) VALUES ('role-930', 'A9300000001', '[\"F9300000001\"]'::jsonb)"
        );
        jdbc.update(
            "INSERT INTO assignment_identity_link (assignment_id, baseline_assignment_uid, target_actor_id, generation) VALUES (?, ?, ?, 0)",
            ASSIGNMENT_ID,
            ASSIGNMENT_UID,
            actorId
        );
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'assignment_changed',
                          'baseline_assignment_observed/v1', 'A9300000001',
                          'assignment', ?, 'system:test', now(), '{}'::jsonb)
                """,
            GRANT_EVENT_ID,
            ASSIGNMENT_ID
        );
        jdbc.update(
            """
                INSERT INTO assignment_grant_projection (
                    assignment_id, source_event_id, role_key,
                    org_unit_id, lifecycle_state
                ) VALUES (?, ?, 'role-930', ?, 'ACTIVE')
                """,
            ASSIGNMENT_ID,
            GRANT_EVENT_ID,
            orgUnitId
        );
    }

    private int submissionValue(String uid) {
        return jdbc.queryForObject(
            "SELECT (form_data ->> 'value')::int FROM data_submission WHERE uid = ?",
            Integer.class,
            uid
        );
    }

    private UUID currentEvent(String uid) {
        return jdbc.queryForObject(
            """
                SELECT current_pointer.source_event_id
                FROM capture_current_projection current_pointer
                JOIN capture_identity_link identity
                  ON identity.capture_id = current_pointer.capture_id
                WHERE identity.baseline_submission_uid = ?
                """,
            UUID.class,
            uid
        );
    }

    private List<JsonNode> capturePayloads(String uid) {
        return jdbc.query(
            """
                SELECT event.payload::text
                FROM event_journal event
                WHERE event.event_type = 'capture'
                  AND event.payload ->> 'captureId' = ?
                ORDER BY event.journal_position
                """,
            (resultSet, rowNumber) -> {
                try {
                    return objectMapper.readTree(resultSet.getString("payload"));
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            },
            CaptureShadowProtocol.captureId(uid).toString()
        );
    }

    private long outboxCount(String uid) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM outbox WHERE submission_uid = ?",
            Long.class,
            uid
        );
    }

    private long captureEventCount() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM event_journal WHERE event_type = 'capture'",
            Long.class
        );
    }

    private long count(String table) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM " + table,
            Long.class
        );
    }

    private void cleanFixtures() {
        properties.getTransition().setCaptureLiveShadowEnabled(false);
        jdbc.execute(
            """
                TRUNCATE TABLE
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
        jdbc.update(
            "DELETE FROM outbox WHERE submission_uid LIKE 'D930%'"
        );
        jdbc.update(
            "DELETE FROM data_submission WHERE uid LIKE 'D930%'"
        );
    }
}
