package org.nmcpye.datarun.jpa.stagesubmission.exception;

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
public class SubmissionCreationException extends RuntimeException {
    public SubmissionCreationException(String notPlannedMode) {
        super(notPlannedMode);
    }
    // TODO Error Mapping: What HTTP status codes & payloads should FlowCreationException produce?
}
