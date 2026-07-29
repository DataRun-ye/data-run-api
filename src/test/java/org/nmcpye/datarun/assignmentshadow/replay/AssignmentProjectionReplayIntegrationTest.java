package org.nmcpye.datarun.assignmentshadow.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowCheckpoint;
import org.nmcpye.datarun.eventjournal.AppendJournalEvent;
import org.nmcpye.datarun.eventjournal.EventContract;
import org.nmcpye.datarun.eventjournal.EventJournalPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class AssignmentProjectionReplayIntegrationTest {

    private static final String ACTIVITY_UID = "A9800000001";
    private static final String ROLE_KEY = "capture-a980";
    private static final UUID ACTOR_ID = UUID.fromString(
        "98000000-0000-4000-8000-000000000001"
    );
    private static final UUID ORG_UNIT_ID = UUID.fromString(
        "98000000-0000-4000-8000-000000000002"
    );
    private static final UUID OBSERVED_ASSIGNMENT_ID = UUID.fromString(
        "98000000-0000-4000-8000-000000000003"
    );
    private static final UUID ENDED_ASSIGNMENT_ID = UUID.fromString(
        "98000000-0000-4000-8000-000000000004"
    );
    private static final Instant T0 = Instant.parse("2026-07-29T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-07-29T09:00:00Z");

    @Autowired
    private AssignmentProjectionReplay replay;

    @Autowired
    private AssignmentShadowCheckpoint checkpoint;

    @Autowired
    private EventJournalPort journal;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clean();
        insertImmutableInputs();
        checkpoint.recordCompleted(T0);
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void validateAndRepairUseFactsOnlyAndPreserveAccessAndJournal() {
        UUID observedEvent = appendObserved(OBSERVED_ASSIGNMENT_ID);
        UUID createdEvent = appendCreated(ENDED_ASSIGNMENT_ID);
        UUID endedEvent = appendEnded(ENDED_ASSIGNMENT_ID);
        insertProjection(OBSERVED_ASSIGNMENT_ID, observedEvent, "ACTIVE");
        insertProjection(ENDED_ASSIGNMENT_ID, endedEvent, "ENDED");

        List<String> journalBefore = journalContent();
        List<String> accessBefore = accessContent();
        AssignmentProjectionReplayReport exact = replay.replay(
            AssignmentProjectionReplayMode.VALIDATE
        );
        assertThat(exact.events()).isEqualTo(3);
        assertThat(exact.assignments()).isEqualTo(2);
        assertThat(exact.rowsRebuilt()).isZero();

        jdbc.update(
            """
                UPDATE assignment_grant_projection
                SET source_event_id = ?, lifecycle_state = 'ACTIVE'
                WHERE assignment_id = ?
                """,
            createdEvent,
            ENDED_ASSIGNMENT_ID
        );
        assertThatThrownBy(() -> replay.replay(
            AssignmentProjectionReplayMode.VALIDATE
        )).isInstanceOf(AssignmentProjectionReplayConflictException.class)
            .hasMessageContaining("differing=1");

        AssignmentProjectionReplayReport repaired = replay.replay(
            AssignmentProjectionReplayMode.REPAIR
        );
        assertThat(repaired.differingBeforeRepair()).isEqualTo(1);
        assertThat(repaired.rowsRebuilt()).isEqualTo(2);
        assertThat(replay.replay(AssignmentProjectionReplayMode.VALIDATE)
            .rowsRebuilt()).isZero();
        assertThat(accessContent()).isEqualTo(accessBefore);
        assertThat(journalContent()).isEqualTo(journalBefore);
        assertThat(jdbc.queryForObject(
            """
                SELECT source_event_id
                FROM assignment_grant_projection
                WHERE assignment_id = ?
                """,
            UUID.class,
            ENDED_ASSIGNMENT_ID
        )).isEqualTo(endedEvent);
    }

    @Test
    void malformedAssignmentShapeFailsBeforeProjectionMutation() {
        ObjectNode payload = createdPayload();
        payload.put("unowned_extension", true);
        UUID eventId = append(
            UUID.randomUUID(),
            EventContract.ASSIGNMENT_CREATED,
            ENDED_ASSIGNMENT_ID,
            T0,
            payload
        );
        insertProjection(ENDED_ASSIGNMENT_ID, eventId, "ACTIVE");
        List<String> projectionBefore = projectionContent();

        assertThatThrownBy(() -> replay.replay(
            AssignmentProjectionReplayMode.REPAIR
        )).isInstanceOf(AssignmentProjectionReplayConflictException.class)
            .hasMessageContaining("payload fields differ");

        assertThat(projectionContent()).isEqualTo(projectionBefore);
    }

    @Test
    void missingBootstrapCheckpointFailsClosed() {
        jdbc.execute("TRUNCATE TABLE transition_checkpoint");
        appendObserved(OBSERVED_ASSIGNMENT_ID);

        assertThatThrownBy(() -> replay.replay(
            AssignmentProjectionReplayMode.VALIDATE
        )).hasMessageContaining("checkpoint is missing");
    }

    private void insertImmutableInputs() {
        jdbc.update(
            "INSERT INTO actor_identity_link (actor_id, baseline_user_uid) VALUES (?, 'U9800000001')",
            ACTOR_ID
        );
        jdbc.update(
            "INSERT INTO org_unit_identity_link (org_unit_id, baseline_org_unit_uid) VALUES (?, 'O9800000001')",
            ORG_UNIT_ID
        );
        jdbc.update(
            """
                INSERT INTO assignment_role_definition (
                    role_key,
                    activity_uid,
                    form_uids
                ) VALUES (?, ?, '["F9800000001"]'::jsonb)
                """,
            ROLE_KEY,
            ACTIVITY_UID
        );
        insertIdentity(OBSERVED_ASSIGNMENT_ID, "S9800000001");
        insertIdentity(ENDED_ASSIGNMENT_ID, "S9800000002");
    }

    private void insertIdentity(UUID assignmentId, String baselineUid) {
        jdbc.update(
            """
                INSERT INTO assignment_identity_link (
                    assignment_id,
                    baseline_assignment_uid,
                    target_actor_id,
                    generation
                ) VALUES (?, ?, ?, 0)
                """,
            assignmentId,
            baselineUid,
            ACTOR_ID
        );
    }

    private UUID appendObserved(UUID assignmentId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("role", ROLE_KEY);
        payload.put("org_unit_id", ORG_UNIT_ID.toString());
        payload.put("lifecycle_state", "ACTIVE");
        return append(
            UUID.randomUUID(),
            EventContract.BASELINE_ASSIGNMENT_OBSERVED,
            assignmentId,
            T0,
            payload
        );
    }

    private UUID appendCreated(UUID assignmentId) {
        return append(
            UUID.randomUUID(),
            EventContract.ASSIGNMENT_CREATED,
            assignmentId,
            T0,
            createdPayload()
        );
    }

    private ObjectNode createdPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putObject("target_actor")
            .put("type", "actor")
            .put("id", ACTOR_ID.toString());
        payload.put("role", ROLE_KEY);
        ObjectNode scope = payload.putObject("scope");
        scope.put("geographic", ORG_UNIT_ID.toString());
        scope.putNull("subject_list");
        scope.putArray("activity").add(ACTIVITY_UID);
        payload.put("valid_from", T0.toString());
        payload.putNull("valid_to");
        return payload;
    }

    private UUID appendEnded(UUID assignmentId) {
        return append(
            UUID.randomUUID(),
            EventContract.ASSIGNMENT_ENDED,
            assignmentId,
            T1,
            objectMapper.createObjectNode().putNull("reason")
        );
    }

    private UUID append(
        UUID eventId,
        String shapeRef,
        UUID assignmentId,
        Instant recordedAt,
        JsonNode payload
    ) {
        journal.append(new AppendJournalEvent(
            eventId,
            EventContract.ASSIGNMENT_CHANGED,
            shapeRef,
            ACTIVITY_UID,
            EventContract.ASSIGNMENT,
            assignmentId,
            "system:test/assignment-replay",
            recordedAt,
            payload
        ));
        return eventId;
    }

    private void insertProjection(
        UUID assignmentId,
        UUID sourceEventId,
        String lifecycleState
    ) {
        jdbc.update(
            """
                INSERT INTO assignment_grant_projection (
                    assignment_id,
                    source_event_id,
                    role_key,
                    org_unit_id,
                    lifecycle_state
                ) VALUES (?, ?, ?, ?, ?)
                """,
            assignmentId,
            sourceEventId,
            ROLE_KEY,
            ORG_UNIT_ID,
            lifecycleState
        );
    }

    private List<String> journalContent() {
        return jdbc.queryForList(
            """
                SELECT event_id::text || '|' || event_type || '|'
                       || shape_ref || '|' || payload::text
                FROM event_journal
                ORDER BY journal_position
                """,
            String.class
        );
    }

    private List<String> projectionContent() {
        return jdbc.queryForList(
            """
                SELECT assignment_id::text || '|' || source_event_id::text
                       || '|' || role_key || '|' || org_unit_id::text
                       || '|' || lifecycle_state
                FROM assignment_grant_projection
                ORDER BY assignment_id
                """,
            String.class
        );
    }

    private List<String> accessContent() {
        return jdbc.queryForList(
            """
                SELECT target_actor_id::text || '|' || activity_uid
                       || '|' || org_unit_id::text || '|' || role_key
                FROM assignment_access_projection
                ORDER BY target_actor_id, activity_uid, org_unit_id, role_key
                """,
            String.class
        );
    }

    private void clean() {
        jdbc.execute(
            """
                TRUNCATE TABLE
                    transition_checkpoint,
                    assignment_grant_projection,
                    assignment_identity_link,
                    assignment_role_definition,
                    org_unit_identity_link,
                    actor_identity_link,
                    event_journal
                RESTART IDENTITY CASCADE
                """
        );
    }
}
