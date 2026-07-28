package org.nmcpye.datarun.assignmentshadow;

import java.util.List;
import java.util.UUID;

public interface AssignmentAccessProjectionPort {

    List<AssignmentAccess> findByTargetActorId(UUID targetActorId);
}
