package org.nmcpye.datarun.captureshadow.bootstrap;

public class CaptureShadowBootstrapMismatchException extends RuntimeException {

    private final CaptureShadowBootstrapReport report;

    public CaptureShadowBootstrapMismatchException(CaptureShadowBootstrapReport report) {
        super("Capture replay comparison found " + report.differenceCount() + " differences");
        this.report = report;
    }

    public CaptureShadowBootstrapReport report() {
        return report;
    }
}
