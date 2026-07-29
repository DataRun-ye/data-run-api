package org.nmcpye.datarun.assignmentshadow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AssignmentCaptureEventReaderIntegrationTest {

    private static final String USER_UID = "Usr00000001";
    private static final String ASSIGNMENT_UID = "Asg00000001";
    private static final String ACTIVITY_UID = "Act00000001";
    private static final String ORG_UNIT_UID = "Org00000001";
    private static final String FORM_UID_1 = "Frm00000001";
    private static final String FORM_UID_2 = "Frm00000002";

    @Autowired
    private AssignmentCaptureEventReadPort reader;

    @Autowired
    private AssignmentShadowCheckpoint checkpoint;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void cleanShadowTables() {
        jdbc.execute("""
            TRUNCATE TABLE
                transition_checkpoint,
                assignment_grant_projection,
                assignment_identity_link,
                assignment_role_definition,
                org_unit_identity_link,
                actor_identity_link,
                event_journal
            RESTART IDENTITY CASCADE
            """);
    }

    @Test
    void batchLookupPreservesAssignmentGenerationRoleAndAliasedScope() {
        checkpoint.recordCompleted(Instant.parse("2026-07-28T10:00:00Z"));
        UUID actorId = AssignmentShadowIdentities.actorId(USER_UID);
        UUID orgUnitId = AssignmentShadowIdentities.orgUnitId(ORG_UNIT_UID);
        jdbc.update(
            "INSERT INTO actor_identity_link (actor_id, baseline_user_uid) VALUES (?, ?)",
            actorId,
            USER_UID
        );
        jdbc.update(
            "INSERT INTO org_unit_identity_link (org_unit_id, baseline_org_unit_uid) VALUES (?, ?)",
            orgUnitId,
            ORG_UNIT_UID
        );
        jdbc.update(
            """
                INSERT INTO assignment_role_definition (
                    role_key,
                    activity_uid,
                    form_uids
                ) VALUES (?, ?, ?::jsonb)
                """,
            "assignment-role:test",
            ACTIVITY_UID,
            "[\"" + FORM_UID_2 + "\",\"" + FORM_UID_1 + "\",\""
                + FORM_UID_1 + "\"]"
        );
        insertGeneration(actorId, orgUnitId, 0, AssignmentLifecycleState.ENDED);
        insertGeneration(actorId, orgUnitId, 1, AssignmentLifecycleState.ACTIVE);

        AssignmentCaptureEventSnapshot result = reader.readAssignments(
            USER_UID,
            Set.of(ASSIGNMENT_UID)
        );

        assertThat(result.status())
            .isEqualTo(AssignmentCaptureEventSnapshot.Status.AVAILABLE);
        assertThat(result.targetActorId()).isEqualTo(actorId);
        assertThat(result.grants())
            .extracting(AssignmentCaptureEventGrant::generation)
            .containsExactly(0, 1);
        assertThat(result.grants())
            .extracting(AssignmentCaptureEventGrant::lifecycleState)
            .containsExactly(
                AssignmentLifecycleState.ENDED,
                AssignmentLifecycleState.ACTIVE
            );
        assertThat(result.grants()).allSatisfy(grant -> {
            assertThat(grant.baselineAssignmentUid())
                .isEqualTo(ASSIGNMENT_UID);
            assertThat(grant.targetActorId()).isEqualTo(actorId);
            assertThat(grant.activityUid()).isEqualTo(ACTIVITY_UID);
            assertThat(grant.orgUnitId()).isEqualTo(orgUnitId);
            assertThat(grant.baselineOrgUnitUid()).isEqualTo(ORG_UNIT_UID);
            assertThat(grant.formUids())
                .containsExactly(FORM_UID_1, FORM_UID_2);
        });
    }

    @Test
    void missingCheckpointIsUnavailableAndMissingActorDoesNotMintAlias() {
        AssignmentCaptureEventSnapshot unavailable =
            reader.readAllForActor(USER_UID);
        assertThat(unavailable.status())
            .isEqualTo(
                AssignmentCaptureEventSnapshot.Status.SHADOW_UNAVAILABLE
            );

        checkpoint.recordCompleted(Instant.parse("2026-07-28T10:00:00Z"));
        AssignmentCaptureEventSnapshot absent =
            reader.readAllForActor(USER_UID);

        assertThat(absent.status())
            .isEqualTo(
                AssignmentCaptureEventSnapshot.Status.ACTOR_ALIAS_ABSENT
            );
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM actor_identity_link",
            Integer.class
        )).isZero();
    }

    @Test
    void shadowReadsUseAnIsolatedReadOnlyTransaction() {
        Transactional transaction = JdbcAssignmentCaptureEventReader.class
            .getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.readOnly()).isTrue();
        assertThat(transaction.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private void insertGeneration(
        UUID actorId,
        UUID orgUnitId,
        int generation,
        AssignmentLifecycleState state
    ) {
        UUID assignmentId = AssignmentShadowIdentities.assignmentId(
            ASSIGNMENT_UID,
            USER_UID,
            generation
        );
        UUID eventId = UUID.randomUUID();
        jdbc.update(
            """
                INSERT INTO assignment_identity_link (
                    assignment_id,
                    baseline_assignment_uid,
                    target_actor_id,
                    generation
                ) VALUES (?, ?, ?, ?)
                """,
            assignmentId,
            ASSIGNMENT_UID,
            actorId,
            generation
        );
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id,
                    event_type,
                    shape_ref,
                    activity_ref,
                    subject_type,
                    subject_id,
                    actor_id,
                    recorded_at,
                    payload
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '{}'::jsonb)
                """,
            eventId,
            "assignment_changed",
            state == AssignmentLifecycleState.ACTIVE
                ? "assignment_created/v1"
                : "assignment_ended/v1",
            ACTIVITY_UID,
            "assignment",
            assignmentId,
            "test:reader",
            Timestamp.from(
                Instant.parse("2026-07-28T11:00:00Z")
                    .plusSeconds(generation)
            )
        );
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
            eventId,
            "assignment-role:test",
            orgUnitId,
            state.name()
        );
    }
}
