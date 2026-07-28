package org.nmcpye.datarun.assignmentshadow;

import java.util.UUID;

public record AssignmentIdentityLink(
    UUID assignmentId,
    String baselineAssignmentUid,
    UUID targetActorId,
    int generation
) {
}
