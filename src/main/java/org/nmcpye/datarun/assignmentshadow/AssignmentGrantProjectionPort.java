package org.nmcpye.datarun.assignmentshadow;

import java.util.Optional;
import java.util.UUID;

interface AssignmentGrantProjectionPort {

    AssignmentGrantProjection insert(AssignmentGrantProjection projection);

    AssignmentGrantProjection update(UUID expectedSourceEventId, AssignmentGrantProjection projection);

    Optional<AssignmentGrantProjection> findByAssignmentId(UUID assignmentId);
}
