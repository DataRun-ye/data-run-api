package org.nmcpye.datarun.jpa.accessfilter;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The assignments a user reaches through the teams his teams manage.
 *
 * <p>A managed team's assignment is in scope only when it carries at least one
 * form the user may use there, which comes from his own team's grants. Other
 * assignments of a managed team stay out of the user's scope, so managers with
 * no such form see nothing new.</p>
 */
@Component
@RequiredArgsConstructor
public class ManagedAssignmentScope {
    private final AssignmentRepository assignmentRepository;
    private final AssignmentFormAccessService formAccessService;

    @Transactional(readOnly = true)
    public List<Assignment> findAccessible(CurrentUserDetails user, boolean includeDisabled) {
        Set<String> managedTeams = user.getManagedTeamsUIDs();
        if (user.isSuper() || managedTeams == null || managedTeams.isEmpty()) {
            return List.of();
        }
        Set<String> grantedForms = user.getFormAccess().stream()
            .filter(access -> managedTeams.contains(access.getTeam()))
            .map(UserFormAccess::getForm)
            .collect(Collectors.toSet());
        if (grantedForms.isEmpty()) {
            return List.of();
        }
        return assignmentRepository.findAllByTeamUidInCarryingAnyForm(managedTeams, grantedForms).stream()
            .filter(assignment -> !Boolean.TRUE.equals(assignment.getDeleted()))
            .filter(assignment -> includeDisabled || isEnabled(assignment))
            .filter(assignment -> !formAccessService.getAccessibleForms(assignment, user).isEmpty())
            .toList();
    }

    private boolean isEnabled(Assignment assignment) {
        return !Boolean.TRUE.equals(assignment.getTeam().getDisabled())
            && !Boolean.TRUE.equals(assignment.getActivity().getDisabled());
    }
}
