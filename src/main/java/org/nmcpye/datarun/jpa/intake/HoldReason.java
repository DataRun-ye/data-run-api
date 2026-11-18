package org.nmcpye.datarun.jpa.intake;

/** Why an upload is held instead of saved (DataRun-ye/data-run-api#42). */
public enum HoldReason {
    /** Started before its campaign's go-live; see {@link GoLivePolicy}. */
    BEFORE_GO_LIVE,
    /** Sent from a practice session (app 6.1.0+, the practice upload route). */
    PRACTICE
}
