package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Component
final class CaptureCheckpointStore {

    enum Status {
        ABSENT,
        EXACT,
        MISMATCH
    }

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    CaptureCheckpointStore(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    Status status(CaptureSourceBoundary boundary) {
        long candidates = count(
            """
                SELECT count(*)
                FROM event_journal
                WHERE event_id = ?
                   OR (
                       shape_ref = ?
                       AND subject_type = ?
                       AND subject_id = ?
                   )
                """,
            CaptureShadowProtocol.CHECKPOINT_EVENT_ID,
            CaptureShadowProtocol.CHECKPOINT_SHAPE_REF,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_TYPE,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_ID
        );
        if (candidates == 0) {
            return Status.ABSENT;
        }
        return candidates == 1 && count(exactSql(), exactArguments(boundary)) == 1
            ? Status.EXACT
            : Status.MISMATCH;
    }

    void insert(CaptureSourceBoundary boundary) {
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id,
                    event_type,
                    shape_ref,
                    activity_ref,
                    subject_type,
                    subject_id,
                    actor_id,
                    recorded_at,
                    payload
                ) VALUES (?, ?, ?, NULL, ?, ?, ?, ?, CAST(? AS jsonb))
                ON CONFLICT (event_id) DO NOTHING
                """,
            CaptureShadowProtocol.CHECKPOINT_EVENT_ID,
            CaptureShadowProtocol.CHECKPOINT_EVENT_TYPE,
            CaptureShadowProtocol.CHECKPOINT_SHAPE_REF,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_TYPE,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_ID,
            CaptureShadowProtocol.SYSTEM_ACTOR,
            Timestamp.from(recordedAt(boundary)),
            payload(boundary)
        );
        if (status(boundary) != Status.EXACT) {
            throw new CaptureShadowBootstrapConflictException(
                "Conflicting immutable capture bootstrap checkpoint"
            );
        }
    }

    private String exactSql() {
        return """
            SELECT count(*)
            FROM event_journal
            WHERE event_id = ?
              AND event_type = ?
              AND shape_ref = ?
              AND activity_ref IS NULL
              AND subject_type = ?
              AND subject_id = ?
              AND actor_id = ?
              AND recorded_at = ?
              AND payload = CAST(? AS jsonb)
            """;
    }

    private Object[] exactArguments(CaptureSourceBoundary boundary) {
        return new Object[]{
            CaptureShadowProtocol.CHECKPOINT_EVENT_ID,
            CaptureShadowProtocol.CHECKPOINT_EVENT_TYPE,
            CaptureShadowProtocol.CHECKPOINT_SHAPE_REF,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_TYPE,
            CaptureShadowProtocol.CHECKPOINT_SUBJECT_ID,
            CaptureShadowProtocol.SYSTEM_ACTOR,
            Timestamp.from(recordedAt(boundary)),
            payload(boundary)
        };
    }

    private Instant recordedAt(CaptureSourceBoundary boundary) {
        return boundary.maximumLastModifiedDate() == null
            ? Instant.EPOCH
            : boundary.maximumLastModifiedDate();
    }

    private String payload(CaptureSourceBoundary boundary) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sourceCount", boundary.sourceCount());
        if (boundary.sourceMaxSerial() == null) {
            payload.putNull("sourceMaxSerial");
        } else {
            payload.put("sourceMaxSerial", boundary.sourceMaxSerial());
        }
        payload.put("sourceSha256", boundary.sourceSha256());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Checkpoint JSON could not be serialized", exception);
        }
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return Objects.requireNonNull(value, "Count query returned null");
    }
}
