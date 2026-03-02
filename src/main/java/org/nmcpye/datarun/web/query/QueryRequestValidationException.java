package org.nmcpye.datarun.web.query;

import org.nmcpye.datarun.common.exceptions.ErrorCodeException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;

public class QueryRequestValidationException extends ErrorCodeException {

    public QueryRequestValidationException(ErrorMessage message) {
        super(message);
    }

    public QueryRequestValidationException(String message) {
        super(new ErrorMessage(ErrorCode.E2050, message));
    }
}

