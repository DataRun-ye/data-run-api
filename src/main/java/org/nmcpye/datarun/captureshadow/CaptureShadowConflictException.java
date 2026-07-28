package org.nmcpye.datarun.captureshadow;

public class CaptureShadowConflictException extends IllegalStateException {

    public CaptureShadowConflictException(String message) {
        super(message);
    }

    public CaptureShadowConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
