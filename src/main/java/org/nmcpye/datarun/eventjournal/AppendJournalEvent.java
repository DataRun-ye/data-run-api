package org.nmcpye.datarun.eventjournal;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AppendJournalEvent(
    UUID eventId,
    String eventType,
    String shapeRef,
    String activityRef,
    String subjectType,
    UUID subjectId,
    String actorId,
    Instant recordedAt,
    JsonNode payload
) {
}
