package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.eventjournal.AppendJournalEvent;
import org.nmcpye.datarun.eventjournal.EventJournalPort;
import org.nmcpye.datarun.eventjournal.JournalEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AssignmentShadowCheckpoint {

    public static final UUID EVENT_ID = AssignmentShadowIdentities.namespacedUuid(
        "datarun-baseline/assignment-shadow/bootstrap-completed/v1"
    );
    public static final UUID SUBJECT_ID = AssignmentShadowIdentities.namespacedUuid(
        "datarun-baseline/assignment-shadow/v1"
    );
    private static final String EVENT_TYPE = "transition_checkpoint";
    private static final String SHAPE_REF = "assignment_shadow_bootstrap_completed/v1";
    private static final String SUBJECT_TYPE = "transition";
    private static final String ACTOR_ID =
        "system:migration/datarun-baseline-assignment-bootstrap";

    private final EventJournalPort journal;
    private final ObjectMapper objectMapper;

    public AssignmentShadowCheckpoint(EventJournalPort journal, ObjectMapper objectMapper) {
        this.journal = journal;
        this.objectMapper = objectMapper;
    }

    public boolean existsAndIsExact() {
        return journal.findByEventId(EVENT_ID).map(this::requireExact).isPresent();
    }

    public void requireCompleted() {
        JournalEvent event = journal.findByEventId(EVENT_ID).orElseThrow(() ->
            new AssignmentAuthorityConflictException(
                "Assignment shadow bootstrap completion checkpoint is missing"
            )
        );
        requireExact(event);
    }

    public void recordCompleted(Instant recordedAt) {
        if (existsAndIsExact()) {
            return;
        }
        journal.append(new AppendJournalEvent(
            EVENT_ID,
            EVENT_TYPE,
            SHAPE_REF,
            null,
            SUBJECT_TYPE,
            SUBJECT_ID,
            ACTOR_ID,
            recordedAt,
            objectMapper.createObjectNode()
        ));
    }

    private JournalEvent requireExact(JournalEvent event) {
        if (!EVENT_TYPE.equals(event.eventType())
            || !SHAPE_REF.equals(event.shapeRef())
            || event.activityRef() != null
            || !SUBJECT_TYPE.equals(event.subjectType())
            || !SUBJECT_ID.equals(event.subjectId())
            || !ACTOR_ID.equals(event.actorId())
            || event.payload() == null
            || !event.payload().isObject()
            || !event.payload().isEmpty()) {
            throw new AssignmentAuthorityConflictException(
                "Assignment shadow bootstrap completion checkpoint conflicts with v1 contract"
            );
        }
        return event;
    }
}
