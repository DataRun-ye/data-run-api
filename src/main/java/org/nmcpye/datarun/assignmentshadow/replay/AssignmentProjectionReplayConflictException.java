package org.nmcpye.datarun.assignmentshadow.replay;

public class AssignmentProjectionReplayConflictException
    extends RuntimeException {

    public AssignmentProjectionReplayConflictException(String message) {
        super(message);
    }

    public AssignmentProjectionReplayConflictException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
