package org.nmcpye.datarun.jpa.datasubmission.validation;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmissionAccessValidatorTest {

    private static final String ASSIGNMENT = "assign00001";
    private static final String TEAM_A = "team0000001";
    private static final String TEAM_B = "team0000002";
    private static final String FORM = "form0000001";

    @Test
    void rejectsPermissionGrantedThroughAnotherTeam() {
        Assignment assignment = assignment();
        AssignmentRepository repository = mock(AssignmentRepository.class);
        when(repository.findByUid(ASSIGNMENT))
            .thenReturn(Optional.of(assignment));
        CurrentUserDetails user = user(TEAM_B);
        DataSubmission submission = submission();

        var validator = new SubmissionAccessValidator(
            new AssignmentFormAccessService(),
            repository);

        assertThatThrownBy(() -> validator.validateAccess(submission, user))
            .isInstanceOf(IllegalQueryException.class);
    }

    @Test
    void acceptsPermissionForTheAssignmentTeamAndForm() {
        Assignment assignment = assignment();
        AssignmentRepository repository = mock(AssignmentRepository.class);
        when(repository.findByUid(ASSIGNMENT))
            .thenReturn(Optional.of(assignment));
        CurrentUserDetails user = user(TEAM_A);
        DataSubmission submission = submission();

        var validator = new SubmissionAccessValidator(
            new AssignmentFormAccessService(),
            repository);

        assertThat(validator.validateAccess(submission, user))
            .isSameAs(submission);
    }

    private Assignment assignment() {
        Team team = new Team();
        team.setUid(TEAM_A);
        Assignment assignment = new Assignment();
        assignment.setUid(ASSIGNMENT);
        assignment.setTeam(team);
        assignment.setForms(Set.of(FORM));
        return assignment;
    }

    private DataSubmission submission() {
        DataSubmission submission = new DataSubmission();
        submission.setUid("submit00001");
        submission.setAssignment(ASSIGNMENT);
        submission.setForm(FORM);
        return submission;
    }

    private CurrentUserDetails user(String permissionTeam) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(false);
        when(user.getUsername()).thenReturn("worker");
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_A));
        when(user.getFormAccess()).thenReturn(List.of(
            UserFormAccess.builder()
                .team(permissionTeam)
                .form(FORM)
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .build()));
        return user;
    }
}
