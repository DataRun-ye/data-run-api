package org.nmcpye.datarun.drun.postgres.domain;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;

import java.util.Set;

import static org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission.*;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class TeamFormPermissions {
    @NotNull
    private String teamUid;
    private String form;
    private Set<FormPermission> permissions;

    public boolean canViewSubmission(String form) {
        return this.form.equals(form) &&
            permissions.stream()
                .anyMatch(FormPermission::canViewSubmission);
    }

    public boolean canViewSubmissionFromUsers(String form) {
        return this.form.equals(form) &&
            permissions.contains(VIEW_SUBMISSIONS_FROM_USERS);
    }

    public boolean canAddSubmission(String form) {
        return this.form.equals(form) &&
            permissions.contains(ADD_SUBMISSIONS);
    }

    public boolean canEditSubmission(String form) {
        return this.form.equals(form) &&
            permissions.contains(EDIT_SUBMISSIONS);

    }

    public boolean canEditSubmissionFromUsers(String form) {
        return this.form.equals(form) &&
            permissions.contains(EDIT_SUBMISSIONS_FROM_USERS);
    }

    public boolean canApproveSubmission(String form) {
        return permissions.contains(APPROVE_SUBMISSIONS);
    }

    public boolean canDeleteSubmission(String form) {
        return this.form.equals(form) &&
            permissions.contains(DELETE_SUBMISSIONS);
    }

    public boolean canDeleteSubmissionFromUsers(String form) {
        return this.form.equals(form) &&
            permissions.contains(DELETE_SUBMISSIONS_FROM_USERS);
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof TeamFormPermissions that)) return false;
//        return Objects.equals(getTeamUid(), that.getTeamUid()) && Objects.equals(getForm(), that.getForm());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(getTeamUid(), getForm());
//    }
}

