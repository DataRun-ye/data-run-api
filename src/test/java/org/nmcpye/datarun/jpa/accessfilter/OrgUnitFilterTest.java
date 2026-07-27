package org.nmcpye.datarun.jpa.accessfilter;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrgUnitFilterTest {

    @Test
    void excludesOrgUnitsFromDisabledTeamsOrActivities() {
        AssignmentRepository repository = mock(AssignmentRepository.class);
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("direct"));
        when(user.getManagedTeamsUIDs()).thenReturn(Set.of("managed"));
        when(repository.findAllByTeamUidIn(anySet())).thenReturn(List.of(
            assignment("enabled", false, false),
            assignment("disabled-team", true, false),
            assignment("disabled-activity", false, true)
        ));

        Set<OrgUnit> result = new OrgUnitFilter(repository).getDirectOrgUnits(user, false);

        assertThat(result).extracting(OrgUnit::getUid).containsExactly("enabled");
        verify(repository).findAllByTeamUidIn(Set.of("direct"));
    }

    private Assignment assignment(String uid, boolean teamDisabled, boolean activityDisabled) {
        Activity activity = new Activity();
        activity.setId(uid + "-activity-id");
        activity.setUid(uid + "-activity");
        activity.setDisabled(activityDisabled);

        Team team = new Team();
        team.setId(uid + "-team-id");
        team.setUid(uid + "-team");
        team.setDisabled(teamDisabled);
        team.setActivity(activity);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setId(uid + "-org-id");
        orgUnit.setUid(uid);

        Assignment assignment = new Assignment();
        assignment.setId(uid + "-assignment-id");
        assignment.setUid(uid + "-assignment");
        assignment.setTeam(team);
        assignment.setActivity(activity);
        assignment.setOrgUnit(orgUnit);
        return assignment;
    }
}
