package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class BaselineAssignmentCaptureAdapter {

    private final CanonicalCaptureFormResolver captureForms;
    private final AssignmentFormAccessService formAccessService;
    private final Clock clock;

    public BaselineAssignmentCaptureAdapter(
        CanonicalCaptureFormResolver captureForms,
        AssignmentFormAccessService formAccessService,
        Clock clock
    ) {
        this.captureForms = captureForms;
        this.formAccessService = formAccessService;
        this.clock = clock;
    }

    public Optional<AssignmentCaptureScope> activeScope(
        CurrentUserDetails user,
        Assignment assignment
    ) {
        return captureScope(user, assignment, false);
    }

    public Optional<AssignmentCaptureScope> deletionTolerantScope(
        CurrentUserDetails user,
        Assignment assignment
    ) {
        return captureScope(user, assignment, true);
    }

    public Optional<AssignmentCaptureScope> structuralScope(
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

    public VersionedUploadDecision decideVersionedUpload(
        CurrentUserDetails user,
        Assignment assignment,
        String formUid
    ) {
        if (user.isSuper()) {
            return VersionedUploadDecision.ADMINISTRATOR_BYPASS;
        }

        String teamUid = assignment.getTeam().getUid();
        if (!user.getUserTeamsUIDs().contains(teamUid)) {
            return VersionedUploadDecision.NOT_DIRECT_TEAM;
        }
        if (!formAccessService.canSubmitData(user, assignment, formUid)) {
            return VersionedUploadDecision.NO_CAPTURE_PERMISSION;
        }
        return VersionedUploadDecision.ALLOWED;
    }

    private Optional<AssignmentCaptureScope> captureScope(
        CurrentUserDetails user,
        Assignment assignment,
        boolean tolerateDeletion
    ) {
        if (user == null || user.isSuper() || assignment == null
            || (!tolerateDeletion && Boolean.TRUE.equals(assignment.getDeleted()))) {
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
            return structuralScope(user, assignment, forms);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private boolean validUid(String value) {
        return CodeGenerator.isValidUid(value);
    }

    public enum VersionedUploadDecision {
        ADMINISTRATOR_BYPASS,
        ALLOWED,
        NOT_DIRECT_TEAM,
        NO_CAPTURE_PERMISSION;

        public boolean accepted() {
            return this == ADMINISTRATOR_BYPASS || this == ALLOWED;
        }
    }
}
