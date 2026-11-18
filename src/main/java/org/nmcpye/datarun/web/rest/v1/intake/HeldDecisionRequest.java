package org.nmcpye.datarun.web.rest.v1.intake;

import org.nmcpye.datarun.jpa.intake.HoldReason;

import java.time.Instant;
import java.util.List;

/**
 * Which held uploads to release or discard: either {@code uids}, or a
 * {@code filter} on one campaign. {@code note} is recorded with the decision.
 */
public record HeldDecisionRequest(List<String> uids, Filter filter, String note) {

    /**
     * @param orgUnit                the org unit and everything under it
     * @param enteredFrom            inclusive; compares the earlier of phone start and server receipt
     * @param enteredTo              exclusive
     * @param acceptedByCurrentRules only uploads the current go-live rules would now save
     */
    public record Filter(
        String activity,
        String orgUnit,
        String form,
        HoldReason reason,
        Instant enteredFrom,
        Instant enteredTo,
        Boolean acceptedByCurrentRules) {
    }
}
