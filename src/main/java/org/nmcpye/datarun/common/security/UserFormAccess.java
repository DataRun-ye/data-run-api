package org.nmcpye.datarun.common.security;

import lombok.Builder;
import lombok.Value;
import org.nmcpye.datarun.common.enumeration.FormPermission;

import java.time.Instant;
import java.util.Set;

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

    public boolean canViewSubmission() {
        return permissions.stream()
                .anyMatch(FormPermission::canViewSubmission);
    }

    public boolean canViewSubmissionFromUsers() {
        return permissions.contains(VIEW_SUBMISSIONS_FROM_USERS);
    }

    public boolean canAddSubmission() {
        return permissions.contains(ADD_SUBMISSIONS);
    }

    public boolean canEditSubmission() {
        return permissions.contains(EDIT_SUBMISSIONS);

    }

    public boolean canEditSubmissionFromUsers() {
        return permissions.contains(EDIT_SUBMISSIONS_FROM_USERS);
    }

    public boolean canApproveSubmission() {
        return permissions.contains(APPROVE_SUBMISSIONS);
    }

    public boolean canDeleteSubmission() {
        return permissions.contains(DELETE_SUBMISSIONS);
    }

    public boolean canDeleteSubmissionFromUsers(String form) {
        return permissions.contains(DELETE_SUBMISSIONS_FROM_USERS);
    }
}

