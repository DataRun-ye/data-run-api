package org.nmcpye.datarun.assignmentshadow;

import java.util.List;
import java.util.UUID;

public record AssignmentCaptureEventGrant(
    String baselineAssignmentUid,
    UUID targetActorId,
    int generation,
    String activityUid,
    UUID orgUnitId,
    String baselineOrgUnitUid,
    List<String> formUids,
    AssignmentLifecycleState lifecycleState
) {
    public AssignmentCaptureEventGrant {
        formUids = List.copyOf(formUids);
    }

    public AssignmentCaptureScope scope() {
        return new AssignmentCaptureScope(
            baselineAssignmentUid,
            targetActorId,
            activityUid,
            orgUnitId,
            formUids
        );
    }

    public boolean matches(
        AssignmentCaptureScope expected,
        String formUid
    ) {
        return baselineAssignmentUid.equals(expected.assignmentUid())
            && targetActorId.equals(expected.targetActorId())
            && activityUid.equals(expected.activityUid())
            && orgUnitId.equals(expected.orgUnitId())
            && formUids.contains(formUid);
    }
}
