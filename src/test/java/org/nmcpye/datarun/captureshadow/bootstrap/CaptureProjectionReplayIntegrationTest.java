package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.assignmentshadow.TransitionIdentityResolver;
import org.nmcpye.datarun.assignmentshadow.VersionedUploadAuthorityReceipt;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.nmcpye.datarun.captureshadow.VersionedCaptureCommand;
import org.nmcpye.datarun.captureshadow.VersionedCaptureSubmissionCommand;
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
class CaptureProjectionReplayIntegrationTest {

    private static final String USER_UID = "U9400000001";
    private static final String ORG_UNIT_UID = "O9400000001";
    private static final String FIRST_UID = "D9400000001";
    private static final String SECOND_UID = "D9400000002";
    private static final String THIRD_UID = "D9400000003";

    @Autowired
    private CaptureProjectionReplay replay;

    @Autowired
    private CaptureShadowBootstrap bootstrap;

    @Autowired
    private CaptureSourceSnapshot sourceSnapshot;

    @Autowired
    private CaptureBootstrapBatchTransaction batchTransaction;

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
        jdbc.update(
            "INSERT INTO org_unit (id, uid, code, name, created_by) VALUES ('replay-org', ?, 'REPLAY', 'Replay org', 'test')",
            ORG_UNIT_UID
        );
    }

    @AfterEach
    void tearDown() {
        properties.getTransition().setCaptureLiveShadowEnabled(false);
        cleanFixtures();
    }

    @Test
    void repairRebuildsBootstrapMixedAndLiveOnlyHeadsWithoutChangingFacts() {
        create(FIRST_UID, 1, false);
        create(SECOND_UID, 2, false);
        bootstrap.run();
        create(FIRST_UID, 11, true);
        create(THIRD_UID, 3, true);
        List<String> journalBefore = journalContent();

        jdbc.update("DELETE FROM capture_current_projection");
        CaptureReplayReport repaired = replay.replay(CaptureReplayMode.REPAIR);
        CaptureReplayReport validated = replay.replay(CaptureReplayMode.VALIDATE);

        assertThat(repaired.captures()).isEqualTo(3);
        assertThat(repaired.events()).isEqualTo(4);
        assertThat(repaired.currentPointersInserted()).isEqualTo(3);
        assertThat(validated.currentPointersInserted()).isZero();
        assertThat(journalContent()).isEqualTo(journalBefore);
        assertThat(currentSubmissionValue(FIRST_UID)).isEqualTo(11);
        assertThat(currentSubmissionValue(SECOND_UID)).isEqualTo(2);
        assertThat(currentSubmissionValue(THIRD_UID)).isEqualTo(3);
        assertThat(previousEventId(FIRST_UID))
            .isEqualTo(CaptureShadowProtocol.captureEventId(FIRST_UID));
        assertThat(previousEventId(THIRD_UID)).isNull();
    }

    @Test
    void bootstrapFactWithoutCompletedCheckpointIsRejected() {
        create(FIRST_UID, 1, false);
        CaptureSourceBoundary boundary = sourceSnapshot.capture();
        batchTransaction.persistNextBatch(
            boundary,
            CaptureShadowBootstrap.INITIAL_SERIAL_CURSOR
        );

        assertReplayFailure("bootstrap fact count");
    }

    @Test
    void postCheckpointBootstrapFactIsRejected() {
        create(FIRST_UID, 1, false);
        bootstrap.run();
        long firstSerial = submissionSerial(FIRST_UID);
        create(SECOND_UID, 2, false);
        batchTransaction.persistNextBatch(
            sourceSnapshot.capture(),
            firstSerial
        );

        assertReplayFailure("bootstrap fact count");
    }

    @Test
    void malformedCheckpointIsRejected() {
        create(FIRST_UID, 1, true);
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, ?, ?, NULL, ?, ?, ?, now(),
                          '{"sourceCount":1,"sourceMaxSerial":null,"sourceSha256":"bad"}'::jsonb)
                """,
            CaptureShadowProtocol.CHECKPOINT_EVENT_ID,
            CaptureShadowProtocol.CHECKPOINT_EVENT_TYPE,
            CaptureShadowProtocol.CHECKPOINT_SHAPE_REF,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_TYPE,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_ID,
            CaptureShadowProtocol.SYSTEM_ACTOR
        );

        assertReplayFailure("checkpoint is malformed");
    }

    @Test
    void forkIsRejected() {
        bootstrapOne();
        UUID root = CaptureShadowProtocol.captureEventId(FIRST_UID);
        insertLiveEvent(
            UUID.fromString("94000000-0000-0000-0000-000000000011"),
            FIRST_UID,
            root
        );
        insertLiveEvent(
            UUID.fromString("94000000-0000-0000-0000-000000000012"),
            FIRST_UID,
            root
        );

        assertReplayFailure("forks");
    }

    @Test
    void missingPredecessorAndOrphanEventAreRejected() {
        bootstrapOne();
        insertLiveEvent(
            UUID.fromString("94000000-0000-0000-0000-000000000021"),
            FIRST_UID,
            UUID.fromString("94000000-0000-0000-0000-000000000099")
        );

        assertReplayFailure("Missing or cross-capture predecessor");

        cleanFixtures();
        jdbc.update(
            "INSERT INTO org_unit (id, uid, code, name, created_by) VALUES ('replay-org', ?, 'REPLAY', 'Replay org', 'test')",
            ORG_UNIT_UID
        );
        bootstrapOne();
        insertOrphanLiveEvent();

        assertReplayFailure("ownership");
    }

    @Test
    void differingPointerAndCurrentStateAreRejected() {
        bootstrapOne();
        create(FIRST_UID, 2, true);
        jdbc.update(
            "UPDATE capture_current_projection SET source_event_id = ? WHERE capture_id = ?",
            CaptureShadowProtocol.captureEventId(FIRST_UID),
            CaptureShadowProtocol.captureId(FIRST_UID)
        );

        assertReplayFailure("pointer differs from derived head");

        cleanFixtures();
        jdbc.update(
            "INSERT INTO org_unit (id, uid, code, name, created_by) VALUES ('replay-org', ?, 'REPLAY', 'Replay org', 'test')",
            ORG_UNIT_UID
        );
        bootstrapOne();
        jdbc.update(
            "UPDATE data_submission SET form_data = '{\"value\":99}'::jsonb WHERE uid = ?",
            FIRST_UID
        );

        assertReplayFailure("head differs from data_submission");
    }

    @Test
    void predecessorInsertedAfterSuccessorIsRejected() {
        bootstrapOne();
        UUID root = CaptureShadowProtocol.captureEventId(FIRST_UID);
        UUID predecessor =
            UUID.fromString("94000000-0000-0000-0000-000000000031");
        UUID successor =
            UUID.fromString("94000000-0000-0000-0000-000000000032");
        insertLiveEvent(successor, FIRST_UID, predecessor);
        insertLiveEvent(predecessor, FIRST_UID, root);
        jdbc.update(
            "UPDATE capture_current_projection SET source_event_id = ? WHERE capture_id = ?",
            successor,
            CaptureShadowProtocol.captureId(FIRST_UID)
        );

        assertReplayFailure("predecessor is not earlier");
    }

    @Test
    void unknownCaptureShapeIsRejected() {
        bootstrapOne();
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'capture', 'unknown_capture/v1', 'A9400000001',
                          'org_unit', ?, 'system:test', now(), '{}'::jsonb)
                """,
            UUID.fromString("94000000-0000-0000-0000-000000000041"),
            TransitionIdentityResolver.orgUnitIdFor(ORG_UNIT_UID)
        );

        assertReplayFailure("Unknown capture shape");
    }

    private void bootstrapOne() {
        create(FIRST_UID, 1, false);
        bootstrap.run();
    }

    private void create(String uid, int value, boolean shadowEnabled) {
        properties.getTransition().setCaptureLiveShadowEnabled(shadowEnabled);
        command.execute(
            List.of(new VersionedCaptureSubmissionCommand(
                submission(uid, value),
                VersionedUploadAuthorityReceipt.administrator(USER_UID)
            )),
            new EntitySaveSummaryVM()
        );
    }

    private DataSubmission submission(String uid, int value) {
        DataSubmission submission = new DataSubmission();
        submission.setUid(uid);
        submission.setDeleted(false);
        submission.setFormData(objectMapper.createObjectNode().put("value", value));
        submission.setForm("F9400000001");
        submission.setFormVersion("V9400000001");
        submission.setVersion(1);
        submission.setAssignment("S9400000001");
        submission.setTeam("T9400000001");
        submission.setOrgUnit(ORG_UNIT_UID);
        submission.setActivity("A9400000001");
        submission.setStartEntryTime(Instant.parse("2026-07-28T01:00:00Z"));
        return submission;
    }

    private void insertLiveEvent(
        UUID eventId,
        String submissionUid,
        UUID previousEventId
    ) {
        UUID actorId = TransitionIdentityResolver.actorIdFor(USER_UID);
        jdbc.update(
            """
                INSERT INTO actor_identity_link (actor_id, baseline_user_uid)
                VALUES (?, ?) ON CONFLICT DO NOTHING
                """,
            actorId,
            USER_UID
        );
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put(
            "captureId",
            CaptureShadowProtocol.captureId(submissionUid).toString()
        );
        if (previousEventId == null) {
            payload.putNull("previousEventId");
        } else {
            payload.put("previousEventId", previousEventId.toString());
        }
        payload.set(
            "acceptance",
            objectMapper.createObjectNode().put("kind", "administrator")
        );
        payload.set("submission", bootstrapSubmission(submissionUid));
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'capture', 'capture_state_accepted/v1',
                          'A9400000001', 'org_unit', ?, ?, now(), CAST(? AS jsonb))
                """,
            eventId,
            TransitionIdentityResolver.orgUnitIdFor(ORG_UNIT_UID),
            actorId.toString(),
            payload.toString()
        );
    }

    private void insertOrphanLiveEvent() {
        UUID orphanCapture =
            UUID.fromString("94000000-0000-0000-0000-000000000051");
        UUID actorId = TransitionIdentityResolver.actorIdFor(USER_UID);
        jdbc.update(
            "INSERT INTO actor_identity_link (actor_id, baseline_user_uid) VALUES (?, ?) ON CONFLICT DO NOTHING",
            actorId,
            USER_UID
        );
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("captureId", orphanCapture.toString());
        payload.putNull("previousEventId");
        payload.set(
            "acceptance",
            objectMapper.createObjectNode().put("kind", "administrator")
        );
        payload.set("submission", bootstrapSubmission(FIRST_UID));
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'capture', 'capture_state_accepted/v1',
                          'A9400000001', 'org_unit', ?, ?, now(), CAST(? AS jsonb))
                """,
            UUID.fromString("94000000-0000-0000-0000-000000000052"),
            TransitionIdentityResolver.orgUnitIdFor(ORG_UNIT_UID),
            actorId.toString(),
            payload.toString()
        );
    }

    private JsonNode bootstrapSubmission(String uid) {
        String value = jdbc.queryForObject(
            "SELECT payload -> 'submission' FROM event_journal WHERE event_id = ?",
            String.class,
            CaptureShadowProtocol.captureEventId(uid)
        );
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private UUID previousEventId(String uid) {
        String value = jdbc.queryForObject(
            """
                SELECT event.payload ->> 'previousEventId'
                FROM capture_current_projection current_pointer
                JOIN event_journal event
                  ON event.event_id = current_pointer.source_event_id
                WHERE current_pointer.capture_id = ?
                """,
            String.class,
            CaptureShadowProtocol.captureId(uid)
        );
        return value == null ? null : UUID.fromString(value);
    }

    private int currentSubmissionValue(String uid) {
        return jdbc.queryForObject(
            """
                SELECT (event.payload -> 'submission' -> 'formData' ->> 'value')::int
                FROM capture_current_projection current_pointer
                JOIN event_journal event
                  ON event.event_id = current_pointer.source_event_id
                WHERE current_pointer.capture_id = ?
                """,
            Integer.class,
            CaptureShadowProtocol.captureId(uid)
        );
    }

    private long submissionSerial(String uid) {
        return jdbc.queryForObject(
            "SELECT serial_number FROM data_submission WHERE uid = ?",
            Long.class,
            uid
        );
    }

    private void assertReplayFailure(String message) {
        assertThatThrownBy(() -> replay.replay(CaptureReplayMode.VALIDATE))
            .isInstanceOf(CaptureReplayConflictException.class)
            .hasMessageContaining(message);
    }

    private List<String> journalContent() {
        return jdbc.queryForList(
            """
                SELECT event_id::text || '|' || event_type || '|' || shape_ref
                       || '|' || payload::text
                FROM event_journal
                ORDER BY journal_position
                """,
            String.class
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
        jdbc.update("DELETE FROM outbox WHERE submission_uid LIKE 'D940%'");
        jdbc.update("DELETE FROM data_submission WHERE uid LIKE 'D940%'");
        jdbc.update("DELETE FROM org_unit WHERE id = 'replay-org'");
    }
}
