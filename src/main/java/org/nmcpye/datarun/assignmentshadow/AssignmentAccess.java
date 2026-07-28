package org.nmcpye.datarun.assignmentshadow;

import java.util.UUID;

public record AssignmentAccess(
    UUID targetActorId,
    String activityUid,
    UUID orgUnitId,
    String roleKey
) {
}
