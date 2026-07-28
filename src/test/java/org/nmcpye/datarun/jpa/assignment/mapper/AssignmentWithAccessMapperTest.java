package org.nmcpye.datarun.jpa.assignment.mapper;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentWithAccessMapperTest {

    @Test
    void projectsExternalUidsAndTeamScopedForms() {
        Activity activity = new Activity();
        activity.setId("activity-internal-id");
        activity.setUid("activ000001");
        Team team = new Team();
        team.setId("team-internal-id");
        team.setUid("team0000001");
        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setId("org-unit-internal-id");
        orgUnit.setUid("orgun000001");

        Assignment assignment = new Assignment();
        assignment.setId("assignment-internal-id");
        assignment.setUid("assign00001");
        assignment.setActivity(activity);
        assignment.setTeam(team);
        assignment.setOrgUnit(orgUnit);
        assignment.setForms(Set.of("form0000001"));

        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(false);
        when(user.getFormAccess()).thenReturn(List.of(
            UserFormAccess.builder()
                .team(team.getUid())
                .form("form0000001")
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .build()));

        var mapper = new AssignmentWithAccessMapper(
            new AssignmentFormAccessService());
        var dto = mapper.toDto(assignment, user);

        assertThat(dto.getId()).isEqualTo(assignment.getUid());
        assertThat(dto.getActivity()).isEqualTo(activity.getUid());
        assertThat(dto.getTeam()).isEqualTo(team.getUid());
        assertThat(dto.getOrgUnit()).isEqualTo(orgUnit.getUid());
        assertThat(dto.getAccessibleForms())
            .singleElement()
            .satisfies(form -> {
                assertThat(form.getAssignment())
                    .isEqualTo(assignment.getUid());
                assertThat(form.getForm()).isEqualTo("form0000001");
                assertThat(form.isCanAddSubmissions()).isTrue();
            });
    }

    @Test
    void eventAuthorizedFormsCannotBeAddedOrRemovedByBaselineAccess() {
        Team team = new Team();
        team.setUid("team0000001");
        Assignment assignment = new Assignment();
        assignment.setUid("assign00001");
        assignment.setTeam(team);
        assignment.setForms(Set.of("form0000001", "form0000002"));
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getFormAccess()).thenReturn(List.of(
            UserFormAccess.builder()
                .team(team.getUid())
                .form("form0000002")
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .build()
        ));
        var mapper = new AssignmentWithAccessMapper(
            new AssignmentFormAccessService()
        );

        var dto = mapper.toDto(
            assignment,
            user,
            List.of("form0000001")
        );

        assertThat(dto.getAccessibleForms())
            .singleElement()
            .satisfies(form -> {
                assertThat(form.getForm()).isEqualTo("form0000001");
                assertThat(form.isCanAddSubmissions()).isFalse();
            });
    }
}
