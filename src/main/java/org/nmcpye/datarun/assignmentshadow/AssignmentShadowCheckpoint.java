package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.transition.TransitionCheckpoint;
import org.nmcpye.datarun.transition.TransitionCheckpointStore;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AssignmentShadowCheckpoint {

    public static final String KEY = "assignment_shadow_bootstrap/v1";

    private final TransitionCheckpointStore checkpoints;
    private final ObjectMapper objectMapper;

    public AssignmentShadowCheckpoint(
        TransitionCheckpointStore checkpoints,
        ObjectMapper objectMapper
    ) {
        this.checkpoints = checkpoints;
        this.objectMapper = objectMapper;
    }

    public boolean existsAndIsExact() {
        return checkpoints.find(KEY).map(this::requireExact).isPresent();
    }

    public void requireCompleted() {
        TransitionCheckpoint checkpoint = checkpoints.find(KEY).orElseThrow(() ->
            new AssignmentAuthorityConflictException(
                "Assignment shadow bootstrap completion checkpoint is missing"
            )
        );
        requireExact(checkpoint);
    }

    public void recordCompleted(Instant recordedAt) {
        if (existsAndIsExact()) {
            return;
        }
        checkpoints.insert(
            KEY,
            recordedAt,
            objectMapper.createObjectNode()
        );
        requireCompleted();
    }

    private TransitionCheckpoint requireExact(
        TransitionCheckpoint checkpoint
    ) {
        if (!KEY.equals(checkpoint.key())
            || checkpoint.recordedAt() == null
            || checkpoint.payload() == null
            || !checkpoint.payload().isObject()
            || !checkpoint.payload().isEmpty()) {
            throw new AssignmentAuthorityConflictException(
                "Assignment shadow bootstrap completion checkpoint conflicts with v1 contract"
            );
        }
        return checkpoint;
    }
}
