package org.nmcpye.datarun.assignmentshadow;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReleasedAssignmentProjectionValidator {

    private final AssignmentAuthoritySnapshot assignmentSnapshot;

    public ReleasedAssignmentProjectionValidator(
        AssignmentAuthoritySnapshot assignmentSnapshot
    ) {
        this.assignmentSnapshot = assignmentSnapshot;
    }

    public void validate(
        String baselineUserUid,
        Map<String, AssignmentCaptureEventGrant> activeGrants
    ) {
        if (activeGrants.isEmpty()) {
            return;
        }

        try {
            Map<AssignmentAuthoritySnapshot.IntentKey,
                AssignmentAuthoritySnapshot.CaptureIntent> projections =
                assignmentSnapshot.baseline(activeGrants.keySet());
            for (AssignmentCaptureEventGrant grant : activeGrants.values()) {
                AssignmentAuthoritySnapshot.IntentKey key =
                    new AssignmentAuthoritySnapshot.IntentKey(
                        grant.baselineAssignmentUid(),
                        baselineUserUid
                    );
                AssignmentAuthoritySnapshot.CaptureIntent expected =
                    new AssignmentAuthoritySnapshot.CaptureIntent(
                        grant.activityUid(),
                        grant.baselineOrgUnitUid(),
                        grant.formUids()
                    );
                if (!expected.equals(projections.get(key))) {
                    throw new AssignmentCaptureAuthorityUnavailableException();
                }
            }
        } catch (AssignmentCaptureAuthorityUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AssignmentCaptureAuthorityUnavailableException();
        }
    }
}
