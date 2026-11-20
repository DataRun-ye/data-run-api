package org.nmcpye.datarun.jpa.datasubmission.validation;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoredSubmissionGuardTest {

    private static final String OWN_TEAM = "team0000001";
    private static final String OTHER_TEAM = "team0000002";
    private static final String FORM = "form0000001";
    private static final String OTHER_FORM = "form0000002";
    private static final String SUBMISSION = "submit00001";

    private final DataSubmissionRepository submissions = mock(DataSubmissionRepository.class);
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final StoredSubmissionGuard guard =
        new StoredSubmissionGuard(submissions, assignments, new AssignmentFormAccessService());

    @Test
    void aNewSubmissionIsNotChecked() {
        when(submissions.findByUid(SUBMISSION)).thenReturn(Optional.empty());

        assertThatCode(() -> guard.check(incoming(FORM, false), user(FormPermission.ADD_SUBMISSIONS)))
            .doesNotThrowAnyException();
    }

    @Test
    void aResendOfARecordTheUserMaySubmitPasses() {
        stored(FORM, assignment("assignOwn01", OWN_TEAM));

        assertThatCode(() -> guard.check(incoming(FORM, false), user(FormPermission.ADD_SUBMISSIONS)))
            .doesNotThrowAnyException();
    }

    @Test
    void aRecordNeverChangesForm() {
        stored(OTHER_FORM, assignment("assignOwn01", OWN_TEAM));

        assertThatThrownBy(() -> guard.check(incoming(FORM, false), user(FormPermission.ADD_SUBMISSIONS)))
            .isInstanceOf(IllegalQueryException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.E4123);
    }

    @Test
    void aRecordOfAnotherTeamCannotBeChanged() {
        stored(FORM, assignment("assignOth01", OTHER_TEAM));

        assertThatThrownBy(() -> guard.check(incoming(FORM, true), user(FormPermission.ADD_SUBMISSIONS)))
            .isInstanceOf(IllegalQueryException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.E4124);
    }

    @Test
    void aRecordWhereTheUserMayNotSubmitThatFormCannotBeChanged() {
        stored(FORM, assignment("assignOwn01", OWN_TEAM));
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(OWN_TEAM));
        when(user.getFormAccess()).thenReturn(List.of(access(OTHER_FORM, FormPermission.ADD_SUBMISSIONS)));

        assertThatThrownBy(() -> guard.check(incoming(FORM, false), user))
            .isInstanceOf(IllegalQueryException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.E4124);
    }

    @Test
    void deletingNeedsTheDeletePermission() {
        stored(FORM, assignment("assignOwn01", OWN_TEAM));

        assertThatThrownBy(() -> guard.check(incoming(FORM, true), user(FormPermission.ADD_SUBMISSIONS)))
            .isInstanceOf(IllegalQueryException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.E4125);
        assertThatCode(() -> guard.check(incoming(FORM, true),
            user(FormPermission.ADD_SUBMISSIONS, FormPermission.DELETE_SUBMISSIONS)))
            .doesNotThrowAnyException();
    }

    @Test
    void administratorsAreNotChecked() {
        CurrentUserDetails administrator = mock(CurrentUserDetails.class);
        when(administrator.isSuper()).thenReturn(true);

        guard.check(incoming(FORM, true), administrator);

        verify(submissions, never()).findByUid(any());
    }

    private void stored(String form, Assignment assignment) {
        DataSubmission stored = new DataSubmission();
        stored.setUid(SUBMISSION);
        stored.setForm(form);
        stored.setAssignment(assignment.getUid());
        when(submissions.findByUid(SUBMISSION)).thenReturn(Optional.of(stored));
        when(assignments.findByUid(assignment.getUid())).thenReturn(Optional.of(assignment));
    }

    private DataSubmission incoming(String form, boolean deleted) {
        DataSubmission incoming = new DataSubmission();
        incoming.setUid(SUBMISSION);
        incoming.setForm(form);
        incoming.setDeleted(deleted);
        return incoming;
    }

    private Assignment assignment(String uid, String teamUid) {
        Team team = new Team();
        team.setUid(teamUid);
        Assignment assignment = new Assignment();
        assignment.setUid(uid);
        assignment.setTeam(team);
        assignment.setForms(Set.of(FORM, OTHER_FORM));
        return assignment;
    }

    private CurrentUserDetails user(FormPermission... permissions) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(OWN_TEAM));
        when(user.getFormAccess()).thenReturn(List.of(access(FORM, permissions)));
        return user;
    }

    private UserFormAccess access(String form, FormPermission... permissions) {
        return UserFormAccess.builder().team(OWN_TEAM).form(form).permissions(Set.of(permissions)).build();
    }
}
