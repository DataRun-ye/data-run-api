package org.nmcpye.datarun.common.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.text.MessageFormat;

public class ErrorMessage {
    private final ErrorCode errorCode;

    private final Object[] args;

    private final String message;

    public ErrorMessage(ErrorCode errorCode, Object... args) {
        this.errorCode = errorCode;
        this.args = args;
        this.message = MessageFormat.format(errorCode.getMessage(), this.args);
    }

    @JsonCreator
    public ErrorMessage(@JsonProperty("message") String message, @JsonProperty("errorCode") ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.args = null;
        this.message = message;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("[%s: '%s']", errorCode.name(), message);
    }
}
