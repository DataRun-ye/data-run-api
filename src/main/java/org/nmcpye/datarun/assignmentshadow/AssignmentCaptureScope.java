package org.nmcpye.datarun.assignmentshadow;

import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

public record AssignmentCaptureScope(
    String assignmentUid,
    UUID targetActorId,
    String activityUid,
    UUID orgUnitId,
    List<String> formUids
) {
    public AssignmentCaptureScope {
        formUids = canonicalForms(formUids);
    }

    private static List<String> canonicalForms(List<String> forms) {
        return List.copyOf(new TreeSet<>(forms));
    }
}
