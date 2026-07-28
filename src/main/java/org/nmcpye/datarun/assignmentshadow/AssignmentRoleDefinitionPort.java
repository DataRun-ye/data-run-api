package org.nmcpye.datarun.assignmentshadow;

import java.util.List;
import java.util.Optional;

public interface AssignmentRoleDefinitionPort {

    AssignmentRoleDefinition resolveOrInsert(String activityUid, List<String> formUids);

    Optional<AssignmentRoleDefinition> findByRoleKey(String roleKey);
}
