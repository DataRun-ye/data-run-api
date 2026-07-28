package org.nmcpye.datarun.assignmentshadow;

import java.util.List;
import java.util.UUID;

public record AssignmentCaptureEventSnapshot(
    Status status,
    UUID targetActorId,
    List<AssignmentCaptureEventGrant> grants
) {
    public AssignmentCaptureEventSnapshot {
        grants = List.copyOf(grants);
    }

    public static AssignmentCaptureEventSnapshot available(
        UUID targetActorId,
        List<AssignmentCaptureEventGrant> grants
    ) {
        return new AssignmentCaptureEventSnapshot(
            Status.AVAILABLE,
            targetActorId,
            grants
        );
    }

    public static AssignmentCaptureEventSnapshot actorAliasAbsent() {
        return new AssignmentCaptureEventSnapshot(
            Status.ACTOR_ALIAS_ABSENT,
            null,
            List.of()
        );
    }

    public static AssignmentCaptureEventSnapshot unavailable() {
        return new AssignmentCaptureEventSnapshot(
            Status.SHADOW_UNAVAILABLE,
            null,
            List.of()
        );
    }

    public enum Status {
        AVAILABLE,
        ACTOR_ALIAS_ABSENT,
        SHADOW_UNAVAILABLE
    }
}
