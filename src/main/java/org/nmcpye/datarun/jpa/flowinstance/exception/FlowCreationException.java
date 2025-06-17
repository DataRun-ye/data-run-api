package org.nmcpye.datarun.jpa.flowinstance.exception;

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
public class FlowCreationException extends RuntimeException {
    public FlowCreationException(String notPlannedMode) {
        super(notPlannedMode);
    }
    // TODO Error Mapping: What HTTP status codes & payloads should FlowCreationException produce?
}
