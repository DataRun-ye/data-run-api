package org.nmcpye.datarun.assignmentshadow;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ReleasedWorkReadScope(
    boolean administrator,
    boolean actorAliasAbsent,
    Map<String, AssignmentCaptureEventGrant> activeGrants,
    Map<String, EmptyCaptureReadCompatibilityAdapter.DisplayAssignment>
        displayCompatibility
) {
    public ReleasedWorkReadScope {
        activeGrants = Map.copyOf(activeGrants);
        displayCompatibility = Map.copyOf(displayCompatibility);
    }

    public static ReleasedWorkReadScope administratorScope() {
        return new ReleasedWorkReadScope(true, false, Map.of(), Map.of());
    }

    public static ReleasedWorkReadScope actorAliasAbsentScope() {
        return new ReleasedWorkReadScope(false, true, Map.of(), Map.of());
    }

    public static ReleasedWorkReadScope fieldUser(
        Map<String, AssignmentCaptureEventGrant> activeGrants,
        Map<String, EmptyCaptureReadCompatibilityAdapter.DisplayAssignment>
            displayCompatibility
    ) {
        return new ReleasedWorkReadScope(
            false,
            false,
            activeGrants,
            displayCompatibility
        );
    }

    public Set<String> assignmentUids() {
        LinkedHashSet<String> uids = new LinkedHashSet<>(
            activeGrants.keySet()
        );
        uids.addAll(displayCompatibility.keySet());
        return Set.copyOf(uids);
    }

    public List<String> formUids(String assignmentUid) {
        AssignmentCaptureEventGrant grant = activeGrants.get(assignmentUid);
        return grant == null ? List.of() : grant.formUids();
    }

    public Optional<AssignmentCaptureEventGrant> activeGrant(
        String assignmentUid
    ) {
        return Optional.ofNullable(activeGrants.get(assignmentUid));
    }

    public Set<String> directOrgUnitUids() {
        LinkedHashSet<String> uids = new LinkedHashSet<>();
        activeGrants.values().stream()
            .map(AssignmentCaptureEventGrant::baselineOrgUnitUid)
            .forEach(uids::add);
        displayCompatibility.values().stream()
            .map(EmptyCaptureReadCompatibilityAdapter.DisplayAssignment::orgUnitUid)
            .filter(uid -> uid != null && !uid.isBlank())
            .forEach(uids::add);
        return Set.copyOf(uids);
    }
}
