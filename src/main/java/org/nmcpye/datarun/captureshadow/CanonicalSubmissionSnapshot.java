package org.nmcpye.datarun.captureshadow;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record CanonicalSubmissionSnapshot(
    String uid,
    Boolean deleted,
    Instant deletedAt,
    JsonNode formData,
    String status,
    String formUid,
    String formVersionUid,
    Integer formVersionNumber,
    String assignmentUid,
    String teamUid,
    String teamCode,
    String orgUnitUid,
    String orgUnitCode,
    String orgUnitName,
    String activityUid,
    Instant startEntryTime,
    Instant finishedEntryTime,
    String createdBy,
    Instant createdDate,
    String lastModifiedBy,
    Instant lastModifiedDate
) {
}
