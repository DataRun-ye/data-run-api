package org.nmcpye.datarun.assignmentshadow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
    value = HttpStatus.SERVICE_UNAVAILABLE,
    reason = "Assignment capture authority is unavailable"
)
public class AssignmentCaptureAuthorityUnavailableException
    extends RuntimeException {

    public AssignmentCaptureAuthorityUnavailableException() {
        super("Assignment capture authority is unavailable");
    }
}
