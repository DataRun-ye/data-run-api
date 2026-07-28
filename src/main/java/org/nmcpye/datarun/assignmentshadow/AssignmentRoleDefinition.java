package org.nmcpye.datarun.assignmentshadow;

import java.util.List;

public record AssignmentRoleDefinition(
    String roleKey,
    String activityUid,
    List<String> formUids
) {
    public AssignmentRoleDefinition {
        formUids = List.copyOf(formUids);
    }
}
