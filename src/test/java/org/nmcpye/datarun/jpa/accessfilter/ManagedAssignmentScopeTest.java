package org.nmcpye.datarun.jpa.accessfilter;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedAssignmentScopeTest {

    private static final String MANAGED = "managed0001";
    private static final String SUPERVISION = "form0000001";
    private static final String HOUSEHOLD = "form0000002";

    private final AssignmentRepository repository = mock(AssignmentRepository.class);
    private final ManagedAssignmentScope scope =
        new ManagedAssignmentScope(repository, new AssignmentFormAccessService());

    @Test
    void keepsManagedAssignmentsThatCarryAFormTheUserMayUse() {
        CurrentUserDetails supervisor = supervisor();
        when(repository.findAllByTeamUidInCarryingAnyForm(Set.of(MANAGED), Set.of(SUPERVISION)))
            .thenReturn(List.of(
                assignment("with-form", Set.of(SUPERVISION, HOUSEHOLD), false, false, false),
                assignment("household-only", Set.of(HOUSEHOLD), false, false, false),
                assignment("retired", Set.of(SUPERVISION), false, false, true),
                assignment("disabled-team", Set.of(SUPERVISION), true, false, false),
                assignment("disabled-activity", Set.of(SUPERVISION), false, true, false)
            ));

        assertThat(scope.findAccessible(supervisor, false))
            .extracting(Assignment::getUid)
            .containsExactly("with-form");
        assertThat(scope.findAccessible(supervisor, true))
            .extracting(Assignment::getUid)
            .containsExactlyInAnyOrder("with-form", "disabled-team", "disabled-activity");
    }

    @Test
    void usersWithoutManagedTeamsReachNothingAndQueryNothing() {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getManagedTeamsUIDs()).thenReturn(Set.of());

        assertThat(scope.findAccessible(user, false)).isEmpty();
        verify(repository, never()).findAllByTeamUidInCarryingAnyForm(any(), any());
    }

    @Test
    void managersWithoutAnyGrantQueryNothing() {
        CurrentUserDetails manager = mock(CurrentUserDetails.class);
        when(manager.getManagedTeamsUIDs()).thenReturn(Set.of(MANAGED));
        when(manager.getFormAccess()).thenReturn(List.of());

        assertThat(scope.findAccessible(manager, false)).isEmpty();
        verify(repository, never()).findAllByTeamUidInCarryingAnyForm(any(), any());
    }

    private CurrentUserDetails supervisor() {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getManagedTeamsUIDs()).thenReturn(Set.of(MANAGED));
        when(user.getFormAccess()).thenReturn(List.of(UserFormAccess.builder()
            .team(MANAGED)
            .form(SUPERVISION)
            .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
            .build()));
        return user;
    }

    private Assignment assignment(
        String uid,
        Set<String> forms,
        boolean teamDisabled,
        boolean activityDisabled,
        boolean deleted
    ) {
        Activity activity = new Activity();
        activity.setUid(uid + "-activity");
        activity.setDisabled(activityDisabled);
        Team team = new Team();
        team.setUid(MANAGED);
        team.setDisabled(teamDisabled);
        team.setActivity(activity);
        Assignment assignment = new Assignment();
        assignment.setUid(uid);
        assignment.setTeam(team);
        assignment.setActivity(activity);
        assignment.setForms(forms);
        assignment.setDeleted(deleted);
        return assignment;
    }
}
