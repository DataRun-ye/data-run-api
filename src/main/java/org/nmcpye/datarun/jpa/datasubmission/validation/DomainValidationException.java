package org.nmcpye.datarun.jpa.datasubmission.validation;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
@SuppressWarnings("unused")
public class DomainValidationException extends IllegalQueryException {
    public DomainValidationException(String msg) {
        super(ErrorCode.E4110, msg);
    }

    /**
     * Constructor. Sets the message based on the error code and arguments.
     *
     * @param errorCode the {@link ErrorCode}.
     * @param args      the message format arguments.
     */
    public DomainValidationException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
