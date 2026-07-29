package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.nmcpye.datarun.transition.TransitionCheckpoint;
import org.nmcpye.datarun.transition.TransitionCheckpointStore;
import org.nmcpye.datarun.transition.TransitionTimestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Component
final class CaptureCheckpointStore {

    private static final Set<String> PAYLOAD_FIELDS = Set.of(
        "sourceCount",
        "sourceMaxSerial",
        "sourceSha256"
    );

    enum Status {
        ABSENT,
        EXACT,
        MISMATCH
    }

    private final TransitionCheckpointStore checkpoints;
    private final ObjectMapper objectMapper;

    CaptureCheckpointStore(
        TransitionCheckpointStore checkpoints,
        ObjectMapper objectMapper
    ) {
        this.checkpoints = checkpoints;
        this.objectMapper = objectMapper;
    }

    Status status(CaptureSourceBoundary boundary) {
        return checkpoints.find(CaptureShadowProtocol.CHECKPOINT_KEY)
            .map(checkpoint -> exact(checkpoint, boundary)
                ? Status.EXACT
                : Status.MISMATCH)
            .orElse(Status.ABSENT);
    }

    void insert(CaptureSourceBoundary boundary) {
        checkpoints.insert(
            CaptureShadowProtocol.CHECKPOINT_KEY,
            recordedAt(boundary),
            payload(boundary)
        );
        if (status(boundary) != Status.EXACT) {
            throw new CaptureShadowBootstrapConflictException(
                "Conflicting immutable capture bootstrap checkpoint"
            );
        }
    }

    private boolean exact(
        TransitionCheckpoint checkpoint,
        CaptureSourceBoundary boundary
    ) {
        return checkpoint.key().equals(CaptureShadowProtocol.CHECKPOINT_KEY)
            && checkpoint.recordedAt().equals(recordedAt(boundary))
            && exactPayload(checkpoint, boundary);
    }

    private boolean exactPayload(
        TransitionCheckpoint checkpoint,
        CaptureSourceBoundary boundary
    ) {
        if (!checkpoint.payload().isObject()) {
            return false;
        }
        Set<String> fields = new HashSet<>();
        checkpoint.payload().fieldNames().forEachRemaining(fields::add);
        if (!fields.equals(PAYLOAD_FIELDS)
            || !checkpoint.payload().path("sourceCount").isIntegralNumber()
            || checkpoint.payload().path("sourceCount").longValue()
                != boundary.sourceCount()
            || !checkpoint.payload().path("sourceSha256").isTextual()
            || !checkpoint.payload().path("sourceSha256").textValue()
                .equals(boundary.sourceSha256())) {
            return false;
        }
        if (boundary.sourceMaxSerial() == null) {
            return checkpoint.payload().path("sourceMaxSerial").isNull();
        }
        return checkpoint.payload().path("sourceMaxSerial").isIntegralNumber()
            && checkpoint.payload().path("sourceMaxSerial").longValue()
                == boundary.sourceMaxSerial();
    }

    private Instant recordedAt(CaptureSourceBoundary boundary) {
        Instant value = boundary.maximumLastModifiedDate() == null
            ? Instant.EPOCH
            : boundary.maximumLastModifiedDate();
        return TransitionTimestamp.toDatabasePrecision(value);
    }

    private ObjectNode payload(CaptureSourceBoundary boundary) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sourceCount", boundary.sourceCount());
        if (boundary.sourceMaxSerial() == null) {
            payload.putNull("sourceMaxSerial");
        } else {
            payload.put("sourceMaxSerial", boundary.sourceMaxSerial());
        }
        payload.put("sourceSha256", boundary.sourceSha256());
        return payload;
    }
}
