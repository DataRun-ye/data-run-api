package org.nmcpye.datarun.assignmentshadow;

import java.util.Collection;

public interface AssignmentCaptureEventReadPort {

    AssignmentCaptureEventSnapshot readAllForActor(String baselineUserUid);

    AssignmentCaptureEventSnapshot readAssignments(
        String baselineUserUid,
        Collection<String> assignmentUids
    );
}
