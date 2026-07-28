package org.nmcpye.datarun.captureshadow.bootstrap;

import java.time.Instant;

record CaptureSourceRow(
    String submissionId,
    long serialNumber,
    String uid,
    Boolean deleted,
    Instant deletedAt,
    String formData,
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
