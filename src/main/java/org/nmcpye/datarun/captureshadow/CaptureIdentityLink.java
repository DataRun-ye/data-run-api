package org.nmcpye.datarun.captureshadow;

import java.util.UUID;

public record CaptureIdentityLink(
    UUID captureId,
    String baselineSubmissionUid,
    String baselineSubmissionId,
    long baselineSerialNumber
) {
}
