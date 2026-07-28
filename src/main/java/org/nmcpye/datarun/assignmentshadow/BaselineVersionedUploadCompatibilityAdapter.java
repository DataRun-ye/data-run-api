package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
public class BaselineVersionedUploadCompatibilityAdapter {

    private final AssignmentCaptureScopeFactory scopeFactory;
    private final CanonicalCaptureFormResolver captureForms;
    private final AssignmentFormAccessService formAccessService;
    private final Clock clock;

    public BaselineVersionedUploadCompatibilityAdapter(
        AssignmentCaptureScopeFactory scopeFactory,
        CanonicalCaptureFormResolver captureForms,
        AssignmentFormAccessService formAccessService,
        Clock clock
    ) {
        this.scopeFactory = scopeFactory;
        this.captureForms = captureForms;
        this.formAccessService = formAccessService;
        this.clock = clock;
    }

    public Optional<AssignmentCaptureScope> retiredScope(
        CurrentUserDetails user,
        Assignment assignment
    ) {
        if (user == null || user.isSuper() || assignment == null
            || !Boolean.TRUE.equals(assignment.getDeleted())) {
            return Optional.empty();
        }

        Team team = assignment.getTeam();
        if (team == null || assignment.getActivity() == null
            || team.getActivity() == null
            || Boolean.TRUE.equals(team.getDisabled())
            || Boolean.TRUE.equals(assignment.getActivity().getDisabled())
            || Boolean.TRUE.equals(team.getActivity().getDisabled())
            || user.getUserTeamsUIDs() == null
            || !user.getUserTeamsUIDs().contains(team.getUid())) {
            return Optional.empty();
        }

        try {
            List<String> forms = captureForms.resolveForActor(
                assignment.getForms(),
                user.getFormAccess(),
                team.getUid(),
                clock.instant()
            );
            if (forms.isEmpty()) {
                return Optional.empty();
            }
            return scopeFactory.fromAssignment(user, assignment, forms);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public DenialClassification classifyDenial(
        CurrentUserDetails user,
        Assignment assignment,
        String formUid
    ) {
        String teamUid = assignment.getTeam().getUid();
        if (user.getUserTeamsUIDs() == null
            || !user.getUserTeamsUIDs().contains(teamUid)) {
            return DenialClassification.NOT_DIRECT_TEAM;
        }
        if (!formAccessService.canSubmitData(user, assignment, formUid)) {
            return DenialClassification.NO_CAPTURE_PERMISSION;
        }
        return DenialClassification.ALLOWED;
    }

    public enum DenialClassification {
        ALLOWED,
        NOT_DIRECT_TEAM,
        NO_CAPTURE_PERMISSION
    }
}
