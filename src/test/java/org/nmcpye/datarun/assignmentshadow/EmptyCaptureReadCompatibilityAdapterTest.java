package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmptyCaptureReadCompatibilityAdapterTest {

    private static final String USER_UID = "Usr00000001";
    private static final String TEAM_UID = "Tem00000001";
    private static final String FORM_UID = "Frm00000001";

    @Test
    void includesOnlyBaselineVisibleAssignmentsWithoutCanonicalCaptureForms() {
        AssignmentRepository repository = mock(AssignmentRepository.class);
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(user.getFormAccess()).thenReturn(List.of(
            UserFormAccess.builder()
                .user(USER_UID)
                .team(TEAM_UID)
                .form(FORM_UID)
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .build()
        ));

        Assignment emptyCapture = assignment(
            "Asg00000001",
            Set.of("Frm00000002")
        );
        Assignment canonicalCapture = assignment(
            "Asg00000002",
            Set.of(FORM_UID)
        );
        Assignment deleted = assignment("Asg00000003", Set.of());
        deleted.setDeleted(true);
        Assignment disabled = assignment("Asg00000004", Set.of());
        disabled.getTeam().setDisabled(true);
        Assignment malformed = assignment("Asg00000005", null);
        when(repository.findAllByTeamUidIn(Set.of(TEAM_UID))).thenReturn(List.of(
            emptyCapture,
            canonicalCapture,
            deleted,
            disabled,
            malformed
        ));
        EmptyCaptureReadCompatibilityAdapter adapter =
            new EmptyCaptureReadCompatibilityAdapter(
                repository,
                new CanonicalCaptureFormResolver(new ObjectMapper()),
                Clock.fixed(
                    Instant.parse("2026-07-28T12:00:00Z"),
                    ZoneOffset.UTC
                )
            );

        Map<String, EmptyCaptureReadCompatibilityAdapter.DisplayAssignment>
            result = adapter.findDisplayAssignments(user);

        assertThat(result).containsOnlyKeys(emptyCapture.getUid());
        assertThat(result.get(emptyCapture.getUid()).orgUnitUid())
            .isEqualTo(emptyCapture.getOrgUnit().getUid());
    }

    private Assignment assignment(String uid, Set<String> forms) {
        Activity activity = new Activity();
        activity.setUid("Act00000001");
        activity.setDisabled(false);

        Team team = new Team();
        team.setUid(TEAM_UID);
        team.setDisabled(false);
        team.setActivity(activity);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setUid("Org00000001");

        Assignment assignment = new Assignment();
        assignment.setUid(uid);
        assignment.setDeleted(false);
        assignment.setActivity(activity);
        assignment.setTeam(team);
        assignment.setOrgUnit(orgUnit);
        assignment.setForms(forms);
        return assignment;
    }
}
