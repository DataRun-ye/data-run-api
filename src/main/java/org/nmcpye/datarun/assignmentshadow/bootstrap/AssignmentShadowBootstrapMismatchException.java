package org.nmcpye.datarun.assignmentshadow.bootstrap;

public class AssignmentShadowBootstrapMismatchException extends IllegalStateException {

    private final AssignmentShadowBootstrapReport report;

    public AssignmentShadowBootstrapMismatchException(AssignmentShadowBootstrapReport report) {
        super("Assignment shadow comparison did not match the baseline");
        this.report = report;
    }

    public AssignmentShadowBootstrapReport report() {
        return report;
    }
}
