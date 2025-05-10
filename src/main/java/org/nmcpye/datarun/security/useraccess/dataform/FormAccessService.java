package org.nmcpye.datarun.security.useraccess.dataform;

import org.apache.commons.collections4.CollectionUtils;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission.*;

/**
 * @author Hamza Assada, 25/04/2025
 */
@Service
@Transactional(readOnly = true)
public class FormAccessService {
    public boolean hasAnyOfPermissions(String formUid, CurrentUserDetails currentUser,
                                       FormPermission... requiredPermission) {
        if (currentUser.isSuper()) {
            return true;
        }

        return currentUser.getFormAccess().stream()
            .filter(ufa -> ufa.getForm().equals(formUid))
            .anyMatch(ufa -> CollectionUtils.containsAny(ufa.getPermissions(), requiredPermission) &&
                (ufa.getValidFrom() == null || ufa.getValidFrom().isBefore(Instant.now())) &&
                (ufa.getValidTo() == null || ufa.getValidTo().isAfter(Instant.now()))
            );
    }

    public boolean canSubmitData(String formUid) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        return hasAnyOfPermissions(formUid, currentUser, ADD_SUBMISSIONS, EDIT_SUBMISSIONS);
    }

    public boolean canViewSubmissions(String form) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }
        return hasAnyOfPermissions(form, SecurityUtils.getCurrentUserDetailsOrThrow(),
            FormPermission.canViewPermissions());
    }

    public boolean canEditSubmissions(String form) {
        return hasAnyOfPermissions(form, SecurityUtils.getCurrentUserDetailsOrThrow(), EDIT_SUBMISSIONS);
    }

    public boolean canAddSubmissions(String form) {
        return hasAnyOfPermissions(form, SecurityUtils.getCurrentUserDetailsOrThrow(), ADD_SUBMISSIONS);
    }

    public boolean canApproveSubmissions(String form) {
        return hasAnyOfPermissions(form, SecurityUtils.getCurrentUserDetailsOrThrow(), APPROVE_SUBMISSIONS);
    }

    public boolean canDeleteSubmissions(String form) {
        return hasAnyOfPermissions(form, SecurityUtils.getCurrentUserDetailsOrThrow(), DELETE_SUBMISSIONS);
    }
}
