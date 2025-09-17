package org.nmcpye.datarun.analytics.projection.exception;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;

/**
 * @author Hamza Assada
 * @since 17/09/2025
 */
public class RepeatExtractionException extends IllegalQueryException {
    public RepeatExtractionException(String message) {
        super(message);
    }

    public RepeatExtractionException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
