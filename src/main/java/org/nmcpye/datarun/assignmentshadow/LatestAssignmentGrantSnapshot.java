package org.nmcpye.datarun.assignmentshadow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record LatestAssignmentGrantSnapshot(
    Status status,
    Map<String, AssignmentCaptureEventGrant> latestGrants
) {
    public LatestAssignmentGrantSnapshot {
        latestGrants = Map.copyOf(latestGrants);
    }

    public static LatestAssignmentGrantSnapshot available(
        Map<String, AssignmentCaptureEventGrant> latestGrants
    ) {
        return new LatestAssignmentGrantSnapshot(Status.AVAILABLE, latestGrants);
    }

    public static LatestAssignmentGrantSnapshot actorAliasAbsent() {
        return new LatestAssignmentGrantSnapshot(
            Status.ACTOR_ALIAS_ABSENT,
            Map.of()
        );
    }

    public static LatestAssignmentGrantSnapshot unavailable() {
        return new LatestAssignmentGrantSnapshot(
            Status.AUTHORITY_UNAVAILABLE,
            Map.of()
        );
    }

    public Optional<AssignmentCaptureEventGrant> latestGrant(
        String assignmentUid
    ) {
        return Optional.ofNullable(latestGrants.get(assignmentUid));
    }

    public Map<String, AssignmentCaptureEventGrant> activeGrants() {
        return grantsInState(AssignmentLifecycleState.ACTIVE);
    }

    public Map<String, AssignmentCaptureEventGrant> endedGrants() {
        return grantsInState(AssignmentLifecycleState.ENDED);
    }

    private Map<String, AssignmentCaptureEventGrant> grantsInState(
        AssignmentLifecycleState state
    ) {
        Map<String, AssignmentCaptureEventGrant> matching =
            new LinkedHashMap<>();
        latestGrants.forEach((assignmentUid, grant) -> {
            if (grant.lifecycleState() == state) {
                matching.put(assignmentUid, grant);
            }
        });
        return Map.copyOf(matching);
    }

    public enum Status {
        AVAILABLE,
        ACTOR_ALIAS_ABSENT,
        AUTHORITY_UNAVAILABLE
    }
}
