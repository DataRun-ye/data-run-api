package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaselineAssignmentCaptureAdapterTest {

    private static final String USER_UID = "Usr00000001";
    private static final String ASSIGNMENT_UID = "Asg00000001";
    private static final String ACTIVITY_UID = "Act00000001";
    private static final String TEAM_UID = "Tem00000001";
    private static final String ORG_UNIT_UID = "Org00000001";
    private static final String FORM_UID_1 = "Frm00000001";
    private static final String FORM_UID_2 = "Frm00000002";
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    private AssignmentFormAccessService formAccessService;
    private BaselineAssignmentCaptureAdapter adapter;
    private CurrentUserDetails user;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        formAccessService = mock(AssignmentFormAccessService.class);
        adapter = new BaselineAssignmentCaptureAdapter(
            new CanonicalCaptureFormResolver(new ObjectMapper()),
            formAccessService,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn(USER_UID);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(user.getFormAccess()).thenReturn(List.of(
            access(FORM_UID_2, FormPermission.EDIT_SUBMISSIONS),
            access(FORM_UID_1, FormPermission.ADD_SUBMISSIONS),
            access("Frm00000003", FormPermission.VIEW_SUBMISSIONS),
            UserFormAccess.builder()
                .team(TEAM_UID)
                .form("Frm00000004")
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .validTo(NOW)
                .build()
        ));
        assignment = assignment(ASSIGNMENT_UID, ORG_UNIT_UID);
    }

    @Test
    void normalizesOnlyCurrentSameTeamCaptureForms() {
        AssignmentCaptureScope scope = adapter.activeScope(user, assignment)
            .orElseThrow();

        assertThat(scope).isEqualTo(new AssignmentCaptureScope(
            ASSIGNMENT_UID,
            AssignmentShadowIdentities.actorId(USER_UID),
            ACTIVITY_UID,
            AssignmentShadowIdentities.orgUnitId(ORG_UNIT_UID),
            List.of(FORM_UID_1, FORM_UID_2)
        ));
    }

    @Test
    void softDeletionIsTheOnlyStatusExceptionForVersionedUploadScope() {
        assignment.setDeleted(true);

        assertThat(adapter.activeScope(user, assignment)).isEmpty();
        assertThat(adapter.deletionTolerantScope(user, assignment)).isPresent();

        assignment.getTeam().setDisabled(true);
        assertThat(adapter.deletionTolerantScope(user, assignment)).isEmpty();

        assignment.getTeam().setDisabled(false);
        assignment.getActivity().setDisabled(true);
        assertThat(adapter.deletionTolerantScope(user, assignment)).isEmpty();

        assignment.getActivity().setDisabled(false);
        assignment.getTeam().getActivity().setDisabled(true);
        assertThat(adapter.deletionTolerantScope(user, assignment)).isEmpty();
    }

    @Test
    void membershipAndPermissionRemainCurrentBaselineDecisions() {
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("Tem00000009"));
        assertThat(adapter.activeScope(user, assignment)).isEmpty();
        assertThat(adapter.decideVersionedUpload(
            user,
            assignment,
            FORM_UID_1
        )).isEqualTo(
            BaselineAssignmentCaptureAdapter.VersionedUploadDecision.NOT_DIRECT_TEAM
        );

        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(formAccessService.canSubmitData(
            user,
            assignment,
            FORM_UID_1
        )).thenReturn(false);
        assertThat(adapter.decideVersionedUpload(
            user,
            assignment,
            FORM_UID_1
        )).isEqualTo(
            BaselineAssignmentCaptureAdapter.VersionedUploadDecision.NO_CAPTURE_PERMISSION
        );

        assignment.setDeleted(true);
        when(formAccessService.canSubmitData(
            user,
            assignment,
            FORM_UID_1
        )).thenReturn(true);
        assertThat(adapter.decideVersionedUpload(
            user,
            assignment,
            FORM_UID_1
        ).accepted()).isTrue();
    }

    private UserFormAccess access(
        String formUid,
        FormPermission permission
    ) {
        return UserFormAccess.builder()
            .user(USER_UID)
            .team(TEAM_UID)
            .form(formUid)
            .permissions(Set.of(permission))
            .build();
    }

    private Assignment assignment(String assignmentUid, String orgUnitUid) {
        Activity activity = new Activity();
        activity.setUid(ACTIVITY_UID);
        activity.setDisabled(false);

        Activity teamActivity = new Activity();
        teamActivity.setUid(ACTIVITY_UID);
        teamActivity.setDisabled(false);

        Team team = new Team();
        team.setUid(TEAM_UID);
        team.setDisabled(false);
        team.setActivity(teamActivity);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setUid(orgUnitUid);

        Assignment value = new Assignment();
        value.setUid(assignmentUid);
        value.setDeleted(false);
        value.setActivity(activity);
        value.setTeam(team);
        value.setOrgUnit(orgUnit);
        value.setForms(Set.of(FORM_UID_2, FORM_UID_1));
        return value;
    }
}
