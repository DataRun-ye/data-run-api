package org.nmcpye.datarun.assignmentshadow;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class AssignmentShadowIdentities {

    private AssignmentShadowIdentities() {
    }

    public static UUID actorId(String userUid) {
        return namespacedUuid("datarun-baseline/actor/" + userUid);
    }

    public static UUID orgUnitId(String orgUnitUid) {
        return namespacedUuid("datarun-baseline/org-unit/" + orgUnitUid);
    }

    public static UUID assignmentId(String assignmentUid, String userUid, int generation) {
        return namespacedUuid(
            "datarun-baseline/assignment/" + assignmentUid
                + "/actor/" + userUid + "/generation/" + generation
        );
    }

    public static UUID namespacedUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
