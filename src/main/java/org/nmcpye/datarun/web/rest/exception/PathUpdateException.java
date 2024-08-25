package org.nmcpye.datarun.web.rest.exception;

public class PathUpdateException extends RuntimeException {

    public PathUpdateException(String message) {
        super(message);
    }

    public PathUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}

