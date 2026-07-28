package org.nmcpye.datarun.assignmentshadow;

public class AssignmentAuthorityConflictException extends IllegalStateException {

    public AssignmentAuthorityConflictException(String message) {
        super(message);
    }

    public AssignmentAuthorityConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
