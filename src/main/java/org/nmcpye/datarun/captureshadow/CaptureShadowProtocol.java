package org.nmcpye.datarun.captureshadow;

import org.nmcpye.datarun.assignmentshadow.AssignmentShadowIdentities;

import java.util.List;
import java.util.UUID;

public final class CaptureShadowProtocol {

    public static final String CAPTURE_ID_NAMESPACE = "datarun-baseline/capture/";
    public static final String CAPTURE_EVENT_ID_NAMESPACE =
        "datarun-baseline/event/submission-captured/";
    public static final String CHECKPOINT_EVENT_NAMESPACE =
        "datarun-baseline/capture-shadow/bootstrap-completed/v1";
    public static final String CHECKPOINT_SUBJECT_NAMESPACE =
        "datarun-baseline/capture-shadow/v1";
    public static final String FINGERPRINT_ENCODING =
        "length-prefixed-utf8-fields/v1";

    public static final String CAPTURE_EVENT_TYPE = "capture";
    public static final String CAPTURE_SHAPE_REF = "baseline_submission_captured/v1";
    public static final String LIVE_CAPTURE_SHAPE_REF =
        "capture_state_accepted/v1";
    public static final String CAPTURE_SUBJECT_TYPE = "org_unit";
    public static final String CHECKPOINT_EVENT_TYPE = "transition_checkpoint";
    public static final String CHECKPOINT_SHAPE_REF =
        "capture_shadow_bootstrap_completed/v1";
    public static final String CHECKPOINT_SUBJECT_TYPE = "transition";
    public static final String SYSTEM_ACTOR =
        "system:migration/datarun-baseline-capture";

    public static final UUID CHECKPOINT_EVENT_ID = namespacedUuid(
        CHECKPOINT_EVENT_NAMESPACE
    );
    public static final UUID CHECKPOINT_SUBJECT_ID = namespacedUuid(
        CHECKPOINT_SUBJECT_NAMESPACE
    );

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
