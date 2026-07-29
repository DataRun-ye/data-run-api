package org.nmcpye.datarun.transition;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record TransitionCheckpoint(
    String key,
    Instant recordedAt,
    JsonNode payload
) {
}
