package org.nmcpye.datarun.jpa.accessfilter;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentFormDto;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentFormAccessServiceTest {

    private static final String TEAM_A = "team0000001";
    private static final String TEAM_B = "team0000002";
    private static final String FORM_A = "form0000001";
    private static final String FORM_B = "form0000002";
    private static final String FORM_C = "form0000003";

    private final AssignmentFormAccessService service =
        new AssignmentFormAccessService();

    @Test
    void scopesVisibleFormsAndActionsToTheAssignmentTeam() {
        Assignment assignment = assignment(
            TEAM_A,
            Set.of(FORM_A, FORM_B, FORM_C));
        CurrentUserDetails user = user(
            false,
            List.of(
                access(
                    TEAM_A,
                    FORM_A,
                    FormPermission.ADD_SUBMISSIONS,
                    FormPermission.EDIT_SUBMISSIONS),
                access(
                    TEAM_B,
                    FORM_B,
                    FormPermission.ADD_SUBMISSIONS),
                access(
                    TEAM_A,
                    FORM_C,
                    FormPermission.DELETE_SUBMISSIONS)));

        var forms = service.getAccessibleForms(assignment, user).stream()
            .collect(Collectors.toMap(
                AssignmentFormDto::getForm,
                Function.identity()));

        assertThat(forms).containsOnlyKeys(FORM_A, FORM_C);
        assertThat(forms.get(FORM_A).isCanAddSubmissions()).isTrue();
        assertThat(forms.get(FORM_A).isCanEditSubmissions()).isTrue();
        assertThat(forms.get(FORM_A).isCanDeleteSubmissions()).isFalse();
        assertThat(forms.get(FORM_C).isCanAddSubmissions()).isFalse();
        assertThat(forms.get(FORM_C).isCanDeleteSubmissions()).isTrue();
        assertThat(service.canSubmitData(user, assignment, FORM_B)).isFalse();
    }

    @Test
    void doesNotAuthorizeAFormThatIsNotAssigned() {
        Assignment assignment = assignment(TEAM_A, Set.of(FORM_A));
        CurrentUserDetails user = user(
            false,
            List.of(access(
                TEAM_A,
                FORM_B,
                FormPermission.ADD_SUBMISSIONS)));

        assertThat(service.canSubmitData(user, assignment, FORM_B)).isFalse();
    }

    @Test
    void administratorsReceiveEveryAssignedFormAndAction() {
        Assignment assignment = assignment(
            TEAM_A,
            Set.of(FORM_A, FORM_B));
        CurrentUserDetails administrator = user(true, List.of());

        assertThat(service.getAccessibleForms(assignment, administrator))
            .allSatisfy(form -> {
                assertThat(form.isCanAddSubmissions()).isTrue();
                assertThat(form.isCanEditSubmissions()).isTrue();
                assertThat(form.isCanDeleteSubmissions()).isTrue();
            })
            .extracting(AssignmentFormDto::getForm)
            .containsExactlyInAnyOrder(FORM_A, FORM_B);
    }

    private Assignment assignment(String teamUid, Set<String> forms) {
        Team team = new Team();
        team.setUid(teamUid);
        Assignment assignment = new Assignment();
        assignment.setUid("assign00001");
        assignment.setTeam(team);
        assignment.setForms(forms);
        return assignment;
    }

    private UserFormAccess access(
        String teamUid,
        String formUid,
        FormPermission... permissions
    ) {
        return UserFormAccess.builder()
            .team(teamUid)
            .form(formUid)
            .permissions(Set.of(permissions))
            .build();
    }

    private CurrentUserDetails user(
        boolean isSuper,
        List<UserFormAccess> formAccess
    ) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(isSuper);
        when(user.getFormAccess()).thenReturn(formAccess);
        return user;
    }
}
