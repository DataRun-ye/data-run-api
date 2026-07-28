package org.nmcpye.datarun.assignmentshadow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReleasedAssignmentProjectionValidatorTest {

    private static final String USER_UID = "Usr00000001";
    private static final String ASSIGNMENT_ONE = "Asg00000001";
    private static final String ASSIGNMENT_TWO = "Asg00000002";

    private final AssignmentAuthoritySnapshot snapshot =
        mock(AssignmentAuthoritySnapshot.class);
    private final ReleasedAssignmentProjectionValidator validator =
        new ReleasedAssignmentProjectionValidator(snapshot);

    @Test
    void validatesAllActiveGrantsWithOneBatchProjectionRead() {
        AssignmentCaptureEventGrant first = grant(
            ASSIGNMENT_ONE,
            "Act00000001",
            "Org00000001",
            List.of("Frm00000001")
        );
        AssignmentCaptureEventGrant second = grant(
            ASSIGNMENT_TWO,
            "Act00000002",
            "Org00000002",
            List.of("Frm00000002")
        );
        Map<String, AssignmentCaptureEventGrant> grants = Map.of(
            ASSIGNMENT_ONE,
            first,
            ASSIGNMENT_TWO,
            second
        );
        when(snapshot.baseline(grants.keySet())).thenReturn(Map.of(
            key(ASSIGNMENT_ONE),
            intent(first),
            key(ASSIGNMENT_TWO),
            intent(second)
        ));

        validator.validate(USER_UID, grants);

        verify(snapshot).baseline(Set.of(ASSIGNMENT_ONE, ASSIGNMENT_TWO));
    }

    @Test
    void missingOrSoftDeletedProjectionFailsAuthorityUnavailable() {
        AssignmentCaptureEventGrant grant = grant(
            ASSIGNMENT_ONE,
            "Act00000001",
            "Org00000001",
            List.of("Frm00000001")
        );
        when(snapshot.baseline(Set.of(ASSIGNMENT_ONE))).thenReturn(Map.of());

        assertThatThrownBy(() ->
            validator.validate(USER_UID, Map.of(ASSIGNMENT_ONE, grant))
        ).isInstanceOf(
            AssignmentCaptureAuthorityUnavailableException.class
        );
    }

    @Test
    void contradictoryTeamActivityOrgUnitOrFormsFailsAuthorityUnavailable() {
        AssignmentCaptureEventGrant grant = grant(
            ASSIGNMENT_ONE,
            "Act00000001",
            "Org00000001",
            List.of("Frm00000001")
        );
        when(snapshot.baseline(Set.of(ASSIGNMENT_ONE))).thenReturn(Map.of(
            key(ASSIGNMENT_ONE),
            new AssignmentAuthoritySnapshot.CaptureIntent(
                "Act00000002",
                "Org00000002",
                List.of("Frm00000002")
            )
        ));

        assertThatThrownBy(() ->
            validator.validate(USER_UID, Map.of(ASSIGNMENT_ONE, grant))
        ).isInstanceOf(
            AssignmentCaptureAuthorityUnavailableException.class
        );
    }

    @Test
    void emptyGrantSetDoesNotReadAssignmentProjections() {
        validator.validate(USER_UID, Map.of());

        verifyNoInteractions(snapshot);
    }

    private AssignmentAuthoritySnapshot.IntentKey key(String assignmentUid) {
        return new AssignmentAuthoritySnapshot.IntentKey(
            assignmentUid,
            USER_UID
        );
    }

    private AssignmentAuthoritySnapshot.CaptureIntent intent(
        AssignmentCaptureEventGrant grant
    ) {
        return new AssignmentAuthoritySnapshot.CaptureIntent(
            grant.activityUid(),
            grant.baselineOrgUnitUid(),
            grant.formUids()
        );
    }

    private AssignmentCaptureEventGrant grant(
        String assignmentUid,
        String activityUid,
        String orgUnitUid,
        List<String> formUids
    ) {
        return new AssignmentCaptureEventGrant(
            assignmentUid,
            AssignmentShadowIdentities.actorId(USER_UID),
            1,
            activityUid,
            AssignmentShadowIdentities.orgUnitId(orgUnitUid),
            orgUnitUid,
            formUids,
            AssignmentLifecycleState.ACTIVE
        );
    }
}
