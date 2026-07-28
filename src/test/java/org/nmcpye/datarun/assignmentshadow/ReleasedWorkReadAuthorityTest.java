package org.nmcpye.datarun.assignmentshadow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.security.CurrentUserDetails;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleasedWorkReadAuthorityTest {

    private static final String USER_UID = "Usr00000001";
    private static final String ASSIGNMENT_UID = "Asg00000001";

    private LatestAssignmentGrantReader latestGrantReader;
    private EmptyCaptureReadCompatibilityAdapter compatibility;
    private ReleasedAssignmentProjectionValidator projectionValidator;
    private CurrentUserDetails user;
    private ReleasedWorkReadAuthority authority;

    @BeforeEach
    void setUp() {
        latestGrantReader = mock(LatestAssignmentGrantReader.class);
        compatibility = mock(EmptyCaptureReadCompatibilityAdapter.class);
        projectionValidator = mock(
            ReleasedAssignmentProjectionValidator.class
        );
        user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn(USER_UID);
        authority = new ReleasedWorkReadAuthority(
            latestGrantReader,
            compatibility,
            projectionValidator
        );
    }

    @Test
    void administratorBypassesEventsAndCompatibility() {
        when(user.isSuper()).thenReturn(true);

        ReleasedWorkReadScope scope = authority.readAll(user);

        assertThat(scope.administrator()).isTrue();
        verify(latestGrantReader, never()).readAllForActor(any());
        verify(compatibility, never()).findDisplayAssignments(any());
    }

    @Test
    void activeGrantsAndDisplayCompatibilityRemainSeparate() {
        AssignmentCaptureEventGrant active = grant(
            ASSIGNMENT_UID,
            AssignmentLifecycleState.ACTIVE
        );
        AssignmentCaptureEventGrant ended = grant(
            "Asg00000002",
            AssignmentLifecycleState.ENDED
        );
        when(latestGrantReader.readAllForActor(USER_UID)).thenReturn(
            LatestAssignmentGrantSnapshot.available(Map.of(
                ASSIGNMENT_UID,
                active,
                ended.baselineAssignmentUid(),
                ended
            ))
        );
        when(compatibility.findDisplayAssignments(user)).thenReturn(Map.of(
            "Asg00000003",
            new EmptyCaptureReadCompatibilityAdapter.DisplayAssignment(
                "Asg00000003",
                "Org00000003"
            ),
            ended.baselineAssignmentUid(),
            new EmptyCaptureReadCompatibilityAdapter.DisplayAssignment(
                ended.baselineAssignmentUid(),
                ended.baselineOrgUnitUid()
            )
        ));

        ReleasedWorkReadScope scope = authority.readAll(user);

        assertThat(scope.assignmentUids()).containsExactlyInAnyOrder(
            ASSIGNMENT_UID,
            "Asg00000003"
        );
        assertThat(scope.formUids(ASSIGNMENT_UID))
            .containsExactly("Frm00000001");
        assertThat(scope.formUids("Asg00000003")).isEmpty();
        assertThat(scope.directOrgUnitUids()).containsExactlyInAnyOrder(
            "Org00000001",
            "Org00000003"
        );
        assertThat(scope.activeGrant("Asg00000002")).isEmpty();
        assertThat(scope.displayCompatibility())
            .doesNotContainKey("Asg00000002");
    }

    @Test
    void absentActorReturnsEmptyWithoutDisplayCompatibility() {
        when(latestGrantReader.readAllForActor(USER_UID)).thenReturn(
            LatestAssignmentGrantSnapshot.actorAliasAbsent()
        );

        ReleasedWorkReadScope scope = authority.readAll(user);

        assertThat(scope.assignmentUids()).isEmpty();
        assertThat(scope.actorAliasAbsent()).isTrue();
        verify(compatibility, never()).findDisplayAssignments(any());
        verify(projectionValidator, never()).validate(any(), any());
    }

    @Test
    void unavailableAuthorityFailsClosed() {
        when(latestGrantReader.readAllForActor(USER_UID)).thenReturn(
            LatestAssignmentGrantSnapshot.unavailable()
        );

        assertThatThrownBy(() -> authority.readAll(user))
            .isInstanceOf(AssignmentCaptureAuthorityUnavailableException.class);
    }

    @Test
    void targetedReadNeverIncludesDisplayCompatibility() {
        AssignmentCaptureEventGrant active = grant(
            ASSIGNMENT_UID,
            AssignmentLifecycleState.ACTIVE
        );
        when(latestGrantReader.readAssignments(
            USER_UID,
            List.of(ASSIGNMENT_UID)
        )).thenReturn(LatestAssignmentGrantSnapshot.available(Map.of(
            ASSIGNMENT_UID,
            active
        )));

        ReleasedWorkReadScope scope = authority.readAssignments(
            user,
            List.of(ASSIGNMENT_UID)
        );

        assertThat(scope.activeGrant(ASSIGNMENT_UID)).contains(active);
        assertThat(scope.displayCompatibility()).isEmpty();
        verify(compatibility, never()).findDisplayAssignments(any());
        verify(projectionValidator).validate(
            USER_UID,
            Map.of(ASSIGNMENT_UID, active)
        );
    }

    private AssignmentCaptureEventGrant grant(
        String assignmentUid,
        AssignmentLifecycleState state
    ) {
        return new AssignmentCaptureEventGrant(
            assignmentUid,
            AssignmentShadowIdentities.actorId(USER_UID),
            0,
            "Act00000001",
            AssignmentShadowIdentities.orgUnitId(
                assignmentUid.equals(ASSIGNMENT_UID)
                    ? "Org00000001"
                    : "Org00000002"
            ),
            assignmentUid.equals(ASSIGNMENT_UID)
                ? "Org00000001"
                : "Org00000002",
            List.of("Frm00000001"),
            state
        );
    }
}
