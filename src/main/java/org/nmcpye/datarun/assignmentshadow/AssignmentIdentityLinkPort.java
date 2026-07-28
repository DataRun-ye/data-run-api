package org.nmcpye.datarun.assignmentshadow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentIdentityLinkPort {

    AssignmentIdentityLink insert(AssignmentIdentityLink identityLink);

    Optional<AssignmentIdentityLink> findByAssignmentId(UUID assignmentId);

    List<AssignmentIdentityLink> findGenerations(String baselineAssignmentUid, UUID targetActorId);
}
