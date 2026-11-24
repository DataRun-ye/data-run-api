package org.nmcpye.datarun.jpa.datasubmission.command;

import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;

import java.util.Objects;

/**
 * The user a submission command acts as.
 *
 * <p>Today's columns store logins, not ids: {@code data_submission.created_by} and {@code last_modified_by},
 * {@code reference_entry.created_by}, {@code held_submission.submitted_by} and {@code decided_by}. JPA auditing
 * writes the first three from the security context, so a command runs in the security context of the user its
 * records name: the requester, or for a held release, the upload's original submitter.</p>
 *
 * @param userId the durable user id ({@code app_user.id})
 * @param login  the login, for the columns above
 * @param access the authorization snapshot the checks read: admin, teams, managed teams and form grants
 */
public record SubmissionActor(String userId, String login, CurrentUserDetails access) {

    public SubmissionActor {
        Objects.requireNonNull(access, "access");
    }

    public static SubmissionActor of(CurrentUserDetails user) {
        return new SubmissionActor(user.getId(), user.getUsername(), user);
    }

    /** The authenticated user of the current request. */
    public static SubmissionActor current() {
        return of(SecurityUtils.getCurrentUserDetailsOrThrow());
    }

    public boolean admin() {
        return access.isSuper();
    }

    /** Ids only: the authorization snapshot also holds the password hash. */
    @Override
    public String toString() {
        return "SubmissionActor[userId=" + userId + ", login=" + login + "]";
    }
}
