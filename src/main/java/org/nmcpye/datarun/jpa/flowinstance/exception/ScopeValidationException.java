package org.nmcpye.datarun.jpa.flowinstance.exception;

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
public class ScopeValidationException extends RuntimeException {
    public ScopeValidationException(String message) {
        super(message);
    }
    // TODO Error Mapping: What HTTP status codes & payloads should FlowCreationException produce?
}
