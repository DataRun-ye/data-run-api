package org.nmcpye.datarun.assignmentshadow;

import java.util.UUID;

public record AssignmentGrantProjection(
    UUID assignmentId,
    UUID sourceEventId,
    String roleKey,
    UUID orgUnitId,
    AssignmentLifecycleState lifecycleState
) {
}
