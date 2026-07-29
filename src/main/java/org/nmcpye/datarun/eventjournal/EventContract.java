package org.nmcpye.datarun.eventjournal;

import java.util.Set;
import java.util.regex.Pattern;

public final class EventContract {

    public static final String CAPTURE = "capture";
    public static final String ASSIGNMENT_CHANGED = "assignment_changed";
    public static final String SUBJECT = "subject";
    public static final String ASSIGNMENT = "assignment";

    public static final String BASELINE_ASSIGNMENT_OBSERVED =
        "baseline_assignment_observed/v1";
    public static final String ASSIGNMENT_CREATED = "assignment_created/v1";
    public static final String ASSIGNMENT_ENDED = "assignment_ended/v1";
    public static final String BASELINE_SUBMISSION_CAPTURED =
        "baseline_submission_captured/v1";
    public static final String CAPTURE_STATE_ACCEPTED =
        "capture_state_accepted/v1";

    private static final Set<String> EVENT_TYPES = Set.of(
        CAPTURE,
        "review",
        "alert",
        "task_created",
        "task_completed",
        ASSIGNMENT_CHANGED
    );
    private static final Set<String> SUBJECT_TYPES = Set.of(
        SUBJECT,
        "actor",
        ASSIGNMENT,
        "process"
    );
    private static final Pattern SHAPE_REF =
        Pattern.compile("^[a-z][a-z0-9_]*/v[0-9]+$");

    private EventContract() {
    }

    public static void requireValid(AppendJournalEvent event) {
        if (event == null || event.eventId() == null) {
            throw invalid("Event identity is required");
        }
        if (!EVENT_TYPES.contains(event.eventType())) {
            throw invalid(
                "Unsupported structural event type: " + event.eventType()
            );
        }
        if (event.shapeRef() == null
            || !SHAPE_REF.matcher(event.shapeRef()).matches()) {
            throw invalid("Invalid event shape reference: " + event.shapeRef());
        }
        if (!SUBJECT_TYPES.contains(event.subjectType())) {
            throw invalid(
                "Unsupported event subject type: " + event.subjectType()
            );
        }
        requireKnownShapeEnvelope(event);
        if (event.subjectId() == null
            || event.actorId() == null
            || event.actorId().isBlank()
            || event.recordedAt() == null
            || event.payload() == null
            || !event.payload().isObject()) {
            throw invalid(
                "Event envelope contains a missing or invalid required value"
            );
        }
    }

    private static void requireKnownShapeEnvelope(AppendJournalEvent event) {
        switch (event.shapeRef()) {
            case BASELINE_ASSIGNMENT_OBSERVED,
                ASSIGNMENT_CREATED,
                ASSIGNMENT_ENDED -> requireEnvelope(
                    event,
                    ASSIGNMENT_CHANGED,
                    ASSIGNMENT
                );
            case BASELINE_SUBMISSION_CAPTURED,
                CAPTURE_STATE_ACCEPTED -> requireEnvelope(
                    event,
                    CAPTURE,
                    SUBJECT
                );
            default -> {
                // Shape registration remains independent from the fixed
                // structural event-type vocabulary.
            }
        }
    }

    private static void requireEnvelope(
        AppendJournalEvent event,
        String expectedType,
        String expectedSubjectType
    ) {
        if (!expectedType.equals(event.eventType())
            || !expectedSubjectType.equals(event.subjectType())) {
            throw invalid(
                "Shape " + event.shapeRef()
                    + " requires event type " + expectedType
                    + " and subject type " + expectedSubjectType
            );
        }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
