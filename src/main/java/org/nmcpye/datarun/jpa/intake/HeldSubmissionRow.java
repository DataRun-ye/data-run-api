package org.nmcpye.datarun.jpa.intake;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * One held upload (table {@code held_submission}).
 *
 * @param uid     the submission's uid
 * @param payload the upload exactly as received; null (and left out of JSON) when not loaded
 */
public record HeldSubmissionRow(
    String uid,
    HoldReason reason,
    HeldState state,
    String activity,
    String orgUnit,
    String orgUnitPath,
    String form,
    String formVersion,
    String team,
    String assignment,
    String submittedBy,
    Instant startedAt,
    Instant receivedAt,
    String goLiveRule,
    Instant liveFrom,
    String decidedBy,
    Instant decidedAt,
    String note,
    String lastReleaseError,
    Instant lastModifiedDate,
    @JsonInclude(JsonInclude.Include.NON_NULL) JsonNode payload) {
}
