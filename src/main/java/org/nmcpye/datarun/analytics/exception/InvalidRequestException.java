package org.nmcpye.datarun.analytics.exception;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;

/**
 * @author Hamza Assada
 * @since 29/08/2025
 */
public class InvalidRequestException extends IllegalQueryException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
