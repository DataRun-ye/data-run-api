package org.nmcpye.datarun.jpa.intake;

import java.time.Instant;
import java.util.Set;

/**
 * Selects held uploads. Every part is optional; {@code orgUnit} includes
 * everything under it. {@code enteredFrom}/{@code enteredTo} (start inclusive,
 * end exclusive) compare the time go-live rules use: the earlier of when the
 * form was started on the phone and when the server received it.
 */
public record HeldFilter(
    String activity,
    String orgUnit,
    String form,
    HoldReason reason,
    Set<HeldState> states,
    Instant enteredFrom,
    Instant enteredTo) {

    public HeldFilter withStates(Set<HeldState> newStates) {
        return new HeldFilter(activity, orgUnit, form, reason, newStates, enteredFrom, enteredTo);
    }
}
