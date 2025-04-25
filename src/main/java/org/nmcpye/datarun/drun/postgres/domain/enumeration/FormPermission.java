package org.nmcpye.datarun.drun.postgres.domain.enumeration;

import java.util.List;

public enum FormPermission {
    /**
     * the User can view his submissions
     */
    VIEW_SUBMISSIONS,
    /**
     * the User can view other users' submissions
     */
    VIEW_SUBMISSIONS_FROM_USERS,
    /**
     * the User can add new submissions
     */
    ADD_SUBMISSIONS,
    /**
     * the User edit his submission after submitting
     */
    EDIT_SUBMISSIONS,
    /**
     * The user can edit other users submissions
     */
    EDIT_SUBMISSIONS_FROM_USERS,
    /**
     * The user can approve other users submissions
     */
    APPROVE_SUBMISSIONS,
    /**
     * The user can delete his submissions after submitting
     */
    DELETE_SUBMISSIONS,
    /**
     * The user can Delete other users submissions
     */
    DELETE_SUBMISSIONS_FROM_USERS;

    public List<FormPermission> canViewPermissions() {
        return List.of(VIEW_SUBMISSIONS, VIEW_SUBMISSIONS_FROM_USERS, EDIT_SUBMISSIONS,
            EDIT_SUBMISSIONS_FROM_USERS, APPROVE_SUBMISSIONS, DELETE_SUBMISSIONS, DELETE_SUBMISSIONS_FROM_USERS);
    }

    public boolean canViewSubmission() {
        return canViewPermissions().contains(this);
    }
}
