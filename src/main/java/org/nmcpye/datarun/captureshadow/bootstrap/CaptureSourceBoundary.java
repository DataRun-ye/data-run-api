package org.nmcpye.datarun.captureshadow.bootstrap;

import java.time.Instant;

public record CaptureSourceBoundary(
    long sourceCount,
    Long sourceMaxSerial,
    String sourceSha256,
    Instant maximumLastModifiedDate
) {

    boolean sameSourceAs(CaptureSourceBoundary other) {
        return other != null
            && sourceCount == other.sourceCount
            && java.util.Objects.equals(sourceMaxSerial, other.sourceMaxSerial)
            && sourceSha256.equals(other.sourceSha256);
    }
}
