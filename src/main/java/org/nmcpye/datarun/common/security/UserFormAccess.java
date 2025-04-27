package org.nmcpye.datarun.common.security;

import lombok.Builder;
import lombok.Value;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;

import java.time.Instant;
import java.util.Set;

import static org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission.*;

/**
 * @author Hamza Assada, 24/04/2025
 */
@Value
@Builder
public class UserFormAccess {
    String user;
    String team;
    String form;
    Set<FormPermission> permissions;
    Instant validFrom;
    Instant validTo;

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
}

