package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EmptyCaptureReadCompatibilityAdapter {

    private final AssignmentRepository assignmentRepository;
    private final CanonicalCaptureFormResolver captureForms;
    private final Clock clock;

    public EmptyCaptureReadCompatibilityAdapter(
        AssignmentRepository assignmentRepository,
        CanonicalCaptureFormResolver captureForms,
        Clock clock
    ) {
        this.assignmentRepository = assignmentRepository;
        this.captureForms = captureForms;
        this.clock = clock;
    }

    public Map<String, DisplayAssignment> findDisplayAssignments(
        CurrentUserDetails user
    ) {
        if (user == null || user.isSuper()) {
            return Map.of();
        }
        Set<String> directTeamUids = user.getUserTeamsUIDs();
        if (directTeamUids == null || directTeamUids.isEmpty()) {
            return Map.of();
        }

        Map<String, DisplayAssignment> compatible = new LinkedHashMap<>();
        assignmentRepository.findAllByTeamUidIn(directTeamUids).stream()
            .filter(assignment -> baselineVisible(assignment, directTeamUids))
            .filter(assignment -> hasNoCanonicalCaptureForms(user, assignment))
            .forEach(assignment -> compatible.putIfAbsent(
                assignment.getUid(),
                new DisplayAssignment(
                    assignment.getUid(),
                    assignment.getOrgUnit() == null
                        ? null
                        : assignment.getOrgUnit().getUid()
                )
            ));
        return Map.copyOf(compatible);
    }

    private boolean baselineVisible(
        Assignment assignment,
        Set<String> directTeamUids
    ) {
        return assignment != null
            && assignment.getUid() != null
            && !Boolean.TRUE.equals(assignment.getDeleted())
            && assignment.getTeam() != null
            && directTeamUids.contains(assignment.getTeam().getUid())
            && !Boolean.TRUE.equals(assignment.getTeam().getDisabled())
            && assignment.getActivity() != null
            && !Boolean.TRUE.equals(assignment.getActivity().getDisabled());
    }

    private boolean hasNoCanonicalCaptureForms(
        CurrentUserDetails user,
        Assignment assignment
    ) {
        try {
            List<String> forms = captureForms.resolveForActor(
                assignment.getForms(),
                user.getFormAccess(),
                assignment.getTeam().getUid(),
                clock.instant()
            );
            return forms.isEmpty();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public record DisplayAssignment(
        String assignmentUid,
        String orgUnitUid
    ) {
    }
}
