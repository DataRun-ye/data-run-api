package org.nmcpye.datarun.captureshadow;

import org.nmcpye.datarun.assignmentshadow.AssignmentShadowIdentities;
import org.nmcpye.datarun.eventjournal.EventContract;

import java.util.List;
import java.util.UUID;

public final class CaptureShadowProtocol {

    public static final String CAPTURE_ID_NAMESPACE = "datarun-baseline/capture/";
    public static final String CAPTURE_EVENT_ID_NAMESPACE =
        "datarun-baseline/event/submission-captured/";
    public static final String FINGERPRINT_ENCODING =
        "length-prefixed-utf8-fields/v1";

    public static final String CAPTURE_EVENT_TYPE = EventContract.CAPTURE;
    public static final String CAPTURE_SHAPE_REF =
        EventContract.BASELINE_SUBMISSION_CAPTURED;
    public static final String LIVE_CAPTURE_SHAPE_REF =
        EventContract.CAPTURE_STATE_ACCEPTED;
    public static final String CAPTURE_SUBJECT_TYPE = EventContract.SUBJECT;
    public static final String CHECKPOINT_KEY = "capture_shadow_bootstrap/v1";
    public static final String SYSTEM_ACTOR =
        "system:migration/datarun-baseline-capture";

    public static final List<String> SOURCE_FIELD_ORDER = List.of(
        "baselineSubmissionId",
        "uid",
        "serialNumber",
        "deleted",
        "deletedAt",
        "formData",
        "status",
        "formUid",
        "formVersionUid",
        "formVersionNumber",
        "assignmentUid",
        "teamUid",
        "teamCode",
        "orgUnitUid",
        "orgUnitCode",
        "orgUnitName",
        "activityUid",
        "startEntryTime",
        "finishedEntryTime",
        "createdBy",
        "createdDate",
        "lastModifiedBy",
        "lastModifiedDate"
    );

    private CaptureShadowProtocol() {
    }

    public static UUID captureId(String submissionUid) {
        return namespacedUuid(CAPTURE_ID_NAMESPACE + submissionUid);
    }

    public static UUID captureEventId(String submissionUid) {
        return namespacedUuid(CAPTURE_EVENT_ID_NAMESPACE + submissionUid);
    }

    private static UUID namespacedUuid(String value) {
        return AssignmentShadowIdentities.namespacedUuid(value);
    }
}
