package org.nmcpye.datarun.jpa.intake;

import java.time.Instant;

/** How many held uploads share an org unit, form, reason and state, and when they were entered. */
public record HeldSummaryRow(
    String orgUnit,
    String orgUnitName,
    String form,
    HoldReason reason,
    HeldState state,
    long count,
    Instant firstEntered,
    Instant lastEntered) {
}
