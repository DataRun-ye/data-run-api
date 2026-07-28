package org.nmcpye.datarun.assignmentshadow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LatestAssignmentGrantReaderTest {

    private static final String USER_UID = "Usr00000001";
    private static final String ASSIGNMENT_UID = "Asg00000001";
    private static final UUID ACTOR_ID =
        AssignmentShadowIdentities.actorId(USER_UID);

    private AssignmentCaptureEventReadPort eventReader;
    private AssignmentShadowCheckpoint checkpoint;
    private LatestAssignmentGrantReader reader;

    @BeforeEach
    void setUp() {
        eventReader = mock(AssignmentCaptureEventReadPort.class);
        checkpoint = mock(AssignmentShadowCheckpoint.class);
        when(checkpoint.existsAndIsExact()).thenReturn(true);
        reader = new LatestAssignmentGrantReader(eventReader, checkpoint);
    }

    @Test
    void highestGenerationAloneIsCurrent() {
        AssignmentCaptureEventGrant ended = grant(
            ASSIGNMENT_UID,
            0,
            AssignmentLifecycleState.ENDED
        );
        AssignmentCaptureEventGrant active = grant(
            ASSIGNMENT_UID,
            1,
            AssignmentLifecycleState.ACTIVE
        );
        AssignmentCaptureEventGrant otherEnded = grant(
            "Asg00000002",
            0,
            AssignmentLifecycleState.ENDED
        );
        when(eventReader.readAllForActor(USER_UID)).thenReturn(snapshot(
            ended,
            active,
            otherEnded
        ));

        LatestAssignmentGrantSnapshot result =
            reader.readAllForActor(USER_UID);

        assertThat(result.status())
            .isEqualTo(LatestAssignmentGrantSnapshot.Status.AVAILABLE);
        assertThat(result.activeGrants()).containsOnlyKeys(ASSIGNMENT_UID);
        assertThat(result.activeGrants().get(ASSIGNMENT_UID)).isSameAs(active);
        assertThat(result.endedGrants()).containsOnlyKeys("Asg00000002");
    }

    @Test
    void duplicateOrContradictoryLatestGenerationIsUnavailable() {
        when(eventReader.readAllForActor(USER_UID)).thenReturn(snapshot(
            grant(ASSIGNMENT_UID, 1, AssignmentLifecycleState.ACTIVE),
            grant(ASSIGNMENT_UID, 1, AssignmentLifecycleState.ENDED)
        ));

        LatestAssignmentGrantSnapshot result =
            reader.readAllForActor(USER_UID);

        assertThat(result.status()).isEqualTo(
            LatestAssignmentGrantSnapshot.Status.AUTHORITY_UNAVAILABLE
        );
        assertThat(result.latestGrants()).isEmpty();

        AssignmentCaptureEventGrant duplicate = grant(
            ASSIGNMENT_UID,
            2,
            AssignmentLifecycleState.ACTIVE
        );
        when(eventReader.readAllForActor(USER_UID)).thenReturn(snapshot(
            duplicate,
            duplicate
        ));
        assertThat(reader.readAllForActor(USER_UID).status()).isEqualTo(
            LatestAssignmentGrantSnapshot.Status.AUTHORITY_UNAVAILABLE
        );
    }

    @Test
    void missingCheckpointAndReaderFailureAreUnavailable() {
        when(checkpoint.existsAndIsExact()).thenReturn(false);

        assertThat(reader.readAllForActor(USER_UID).status()).isEqualTo(
            LatestAssignmentGrantSnapshot.Status.AUTHORITY_UNAVAILABLE
        );
        verify(eventReader, never()).readAllForActor(any());

        when(checkpoint.existsAndIsExact()).thenReturn(true);
        when(eventReader.readAllForActor(USER_UID))
            .thenThrow(new IllegalStateException("read failed"));
        assertThat(reader.readAllForActor(USER_UID).status()).isEqualTo(
            LatestAssignmentGrantSnapshot.Status.AUTHORITY_UNAVAILABLE
        );
    }

    @Test
    void absentActorAliasRemainsDistinctFromUnavailableAuthority() {
        when(eventReader.readAssignments(USER_UID, Set.of(ASSIGNMENT_UID)))
            .thenReturn(AssignmentCaptureEventSnapshot.actorAliasAbsent());

        LatestAssignmentGrantSnapshot result = reader.readAssignments(
            USER_UID,
            Set.of(ASSIGNMENT_UID)
        );

        assertThat(result.status()).isEqualTo(
            LatestAssignmentGrantSnapshot.Status.ACTOR_ALIAS_ABSENT
        );
        assertThat(result.latestGrants()).isEmpty();
    }

    private AssignmentCaptureEventSnapshot snapshot(
        AssignmentCaptureEventGrant... grants
    ) {
        return AssignmentCaptureEventSnapshot.available(
            ACTOR_ID,
            List.of(grants)
        );
    }

    private AssignmentCaptureEventGrant grant(
        String assignmentUid,
        int generation,
        AssignmentLifecycleState state
    ) {
        return new AssignmentCaptureEventGrant(
            assignmentUid,
            ACTOR_ID,
            generation,
            "Act00000001",
            AssignmentShadowIdentities.orgUnitId("Org00000001"),
            "Org00000001",
            List.of("Frm00000001"),
            state
        );
    }
}
