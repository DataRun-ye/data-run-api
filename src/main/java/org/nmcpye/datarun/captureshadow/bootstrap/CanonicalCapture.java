package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

record CanonicalCapture(
    CaptureSourceRow source,
    UUID captureId,
    UUID eventId,
    UUID orgUnitId,
    Instant recordedAt,
    ObjectNode payload
) {
}
