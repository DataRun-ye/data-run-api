package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ReleasedWorkReadAuthority {

    private final LatestAssignmentGrantReader latestGrantReader;
    private final EmptyCaptureReadCompatibilityAdapter compatibility;
    private final ReleasedAssignmentProjectionValidator projectionValidator;

    public ReleasedWorkReadAuthority(
        LatestAssignmentGrantReader latestGrantReader,
        EmptyCaptureReadCompatibilityAdapter compatibility,
        ReleasedAssignmentProjectionValidator projectionValidator
    ) {
        this.latestGrantReader = latestGrantReader;
        this.compatibility = compatibility;
        this.projectionValidator = projectionValidator;
    }

    public ReleasedWorkReadScope readAll(CurrentUserDetails user) {
        Objects.requireNonNull(user);
        if (user.isSuper()) {
            return ReleasedWorkReadScope.administratorScope();
        }
        LatestAssignmentGrantSnapshot snapshot =
            latestGrantReader.readAllForActor(user.getUid());
        return fieldScope(user, snapshot, true);
    }

    public ReleasedWorkReadScope readAssignments(
        CurrentUserDetails user,
        Collection<String> assignmentUids
    ) {
        Objects.requireNonNull(user);
        if (user.isSuper()) {
            return ReleasedWorkReadScope.administratorScope();
        }
        LatestAssignmentGrantSnapshot snapshot =
            latestGrantReader.readAssignments(user.getUid(), assignmentUids);
        return fieldScope(user, snapshot, false);
    }

    private ReleasedWorkReadScope fieldScope(
        CurrentUserDetails user,
        LatestAssignmentGrantSnapshot snapshot,
        boolean includeDisplayCompatibility
    ) {
        if (snapshot.status()
            == LatestAssignmentGrantSnapshot.Status.AUTHORITY_UNAVAILABLE) {
            throw new AssignmentCaptureAuthorityUnavailableException();
        }
        if (snapshot.status()
            == LatestAssignmentGrantSnapshot.Status.ACTOR_ALIAS_ABSENT) {
            return ReleasedWorkReadScope.actorAliasAbsentScope();
        }
        Map<String, AssignmentCaptureEventGrant> activeGrants =
            snapshot.activeGrants();
        projectionValidator.validate(user.getUid(), activeGrants);
        Map<String, EmptyCaptureReadCompatibilityAdapter.DisplayAssignment>
            displayCompatibility = new LinkedHashMap<>();
        if (includeDisplayCompatibility) {
            displayCompatibility.putAll(
                compatibility.findDisplayAssignments(user)
            );
            snapshot.latestGrants().keySet().forEach(
                displayCompatibility::remove
            );
        }
        return ReleasedWorkReadScope.fieldUser(
            activeGrants,
            displayCompatibility
        );
    }
}
