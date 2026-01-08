package org.nmcpye.datarun.analytics.exception;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidMeasureException extends IllegalQueryException {
    public InvalidMeasureException(String message) {
        super(message);
    }

    public InvalidMeasureException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
