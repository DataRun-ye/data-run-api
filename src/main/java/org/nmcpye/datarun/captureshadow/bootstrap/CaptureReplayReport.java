package org.nmcpye.datarun.captureshadow.bootstrap;

public record CaptureReplayReport(
    long captures,
    long events,
    long currentPointersInserted
) {
    public String toOperatorText() {
        return "capture-shadow-replay/v1\n"
            + "status=EXACT\n"
            + "captures=" + captures + "\n"
            + "events=" + events + "\n"
            + "current_pointers_inserted=" + currentPointersInserted + "\n";
    }
}
