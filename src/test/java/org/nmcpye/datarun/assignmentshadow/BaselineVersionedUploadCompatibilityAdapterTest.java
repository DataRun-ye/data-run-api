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

class BaselineVersionedUploadCompatibilityAdapterTest {

    private static final String USER_UID = "Usr00000001";
    private static final String ASSIGNMENT_UID = "Asg00000001";
    private static final String ACTIVITY_UID = "Act00000001";
    private static final String TEAM_UID = "Tem00000001";
    private static final String ORG_UNIT_UID = "Org00000001";
    private static final String FORM_UID = "Frm00000001";
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    private AssignmentFormAccessService formAccessService;
    private BaselineVersionedUploadCompatibilityAdapter adapter;
    private CurrentUserDetails user;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        CanonicalCaptureFormResolver captureForms =
            new CanonicalCaptureFormResolver(new ObjectMapper());
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        formAccessService = mock(AssignmentFormAccessService.class);
        AssignmentCaptureScopeFactory scopeFactory =
            new AssignmentCaptureScopeFactory();
        adapter = new BaselineVersionedUploadCompatibilityAdapter(
            scopeFactory,
            captureForms,
            formAccessService,
            clock
        );

        user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn(USER_UID);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(user.getFormAccess()).thenReturn(List.of(captureAccess()));
        assignment = assignment();
    }

    @Test
    void retiredScopeRequiresCurrentMembershipPermissionAndActiveScope() {
        assertThat(adapter.retiredScope(user, assignment)).isPresent();

        when(user.getUserTeamsUIDs()).thenReturn(Set.of("Tem00000009"));
        assertThat(adapter.retiredScope(user, assignment)).isEmpty();
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));

        when(user.getFormAccess()).thenReturn(List.of());
        assertThat(adapter.retiredScope(user, assignment)).isEmpty();
        when(user.getFormAccess()).thenReturn(List.of(captureAccess()));

        assignment.getTeam().setDisabled(true);
        assertThat(adapter.retiredScope(user, assignment)).isEmpty();
        assignment.getTeam().setDisabled(false);

        assignment.getActivity().setDisabled(true);
        assertThat(adapter.retiredScope(user, assignment)).isEmpty();
        assignment.getActivity().setDisabled(false);

        assignment.getTeam().getActivity().setDisabled(true);
        assertThat(adapter.retiredScope(user, assignment)).isEmpty();
        assignment.getTeam().getActivity().setDisabled(false);

        assignment.setForms(Set.of("Frm00000009"));
        assertThat(adapter.retiredScope(user, assignment)).isEmpty();
    }

    @Test
    void retiredScopeNeverAppliesToAnActiveAssignment() {
        assignment.setDeleted(false);

        assertThat(adapter.retiredScope(user, assignment)).isEmpty();
    }

    @Test
    void denialClassificationPreservesReleasedWireCategories() {
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("Tem00000009"));
        assertThat(adapter.classifyDenial(user, assignment, FORM_UID))
            .isEqualTo(
                BaselineVersionedUploadCompatibilityAdapter
                    .DenialClassification.NOT_DIRECT_TEAM
            );

        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(false);
        assertThat(adapter.classifyDenial(user, assignment, FORM_UID))
            .isEqualTo(
                BaselineVersionedUploadCompatibilityAdapter
                    .DenialClassification.NO_CAPTURE_PERMISSION
            );

        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);
        assertThat(adapter.classifyDenial(user, assignment, FORM_UID))
            .isEqualTo(
                BaselineVersionedUploadCompatibilityAdapter
                    .DenialClassification.ALLOWED
            );
    }

    private UserFormAccess captureAccess() {
        return UserFormAccess.builder()
            .user(USER_UID)
            .team(TEAM_UID)
            .form(FORM_UID)
            .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
            .build();
    }

    private Assignment assignment() {
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
        orgUnit.setUid(ORG_UNIT_UID);

        Assignment value = new Assignment();
        value.setUid(ASSIGNMENT_UID);
        value.setDeleted(true);
        value.setActivity(activity);
        value.setTeam(team);
        value.setOrgUnit(orgUnit);
        value.setForms(Set.of(FORM_UID));
        return value;
    }
}
