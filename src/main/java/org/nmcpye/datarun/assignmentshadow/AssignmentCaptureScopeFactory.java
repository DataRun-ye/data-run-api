package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class AssignmentCaptureScopeFactory {

    public Optional<AssignmentCaptureScope> fromAssignment(
        CurrentUserDetails user,
        Assignment assignment,
        Collection<String> forms
    ) {
        if (user == null || assignment == null || assignment.getTeam() == null
            || assignment.getActivity() == null
            || assignment.getTeam().getActivity() == null
            || assignment.getOrgUnit() == null
            || !validUid(user.getUid())
            || !validUid(assignment.getUid())
            || !validUid(assignment.getTeam().getUid())
            || !validUid(assignment.getActivity().getUid())
            || !validUid(assignment.getTeam().getActivity().getUid())
            || !validUid(assignment.getOrgUnit().getUid())
            || forms == null
            || forms.stream().anyMatch(form -> !validUid(form))) {
            return Optional.empty();
        }

        return Optional.of(new AssignmentCaptureScope(
            assignment.getUid(),
            AssignmentShadowIdentities.actorId(user.getUid()),
            assignment.getActivity().getUid(),
            AssignmentShadowIdentities.orgUnitId(
                assignment.getOrgUnit().getUid()
            ),
            List.copyOf(forms)
        ));
    }

    private boolean validUid(String value) {
        return CodeGenerator.isValidUid(value);
    }
}
