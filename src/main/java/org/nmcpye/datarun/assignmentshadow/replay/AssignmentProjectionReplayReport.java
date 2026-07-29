package org.nmcpye.datarun.assignmentshadow.replay;

public record AssignmentProjectionReplayReport(
    long events,
    long assignments,
    long missingBeforeRepair,
    long unexpectedBeforeRepair,
    long differingBeforeRepair,
    long rowsRebuilt
) {
    public String toOperatorText() {
        return "assignment-projection-replay/v1\n"
            + "status=EXACT\n"
            + "events=" + events + "\n"
            + "assignments=" + assignments + "\n"
            + "missing_before_repair=" + missingBeforeRepair + "\n"
            + "unexpected_before_repair=" + unexpectedBeforeRepair + "\n"
            + "differing_before_repair=" + differingBeforeRepair + "\n"
            + "rows_rebuilt=" + rowsRebuilt + "\n";
    }
}
