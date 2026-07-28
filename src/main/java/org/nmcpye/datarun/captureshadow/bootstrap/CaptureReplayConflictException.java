package org.nmcpye.datarun.captureshadow.bootstrap;

import org.nmcpye.datarun.captureshadow.CaptureShadowConflictException;

public class CaptureReplayConflictException
    extends CaptureShadowConflictException {

    public CaptureReplayConflictException(String message) {
        super(message);
    }

    public CaptureReplayConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
