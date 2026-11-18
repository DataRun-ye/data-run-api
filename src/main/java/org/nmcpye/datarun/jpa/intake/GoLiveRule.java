package org.nmcpye.datarun.jpa.intake;

import java.time.Instant;

/**
 * When real data starts for one scope of a campaign (DataRun-ye/data-run-api#42).
 * The scope is the campaign ({@code activity}), optionally narrowed to an org
 * unit and everything under it, and/or to one form. There is one rule per scope;
 * {@link GoLivePolicy} decides which rule applies to a submission.
 *
 * @param orgUnit null for the whole campaign
 * @param form    null for every form of the campaign
 */
public record GoLiveRule(
    String uid,
    String activity,
    String orgUnit,
    String form,
    Instant liveFrom,
    String note,
    String lastModifiedBy,
    Instant lastModifiedDate) {
}
