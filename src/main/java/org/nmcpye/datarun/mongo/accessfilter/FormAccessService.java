package org.nmcpye.datarun.mongo.accessfilter;

import org.apache.commons.collections4.CollectionUtils;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentFormDto;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.common.enumeration.FormPermission.*;

/**
 * @author Hamza Assada 25/04/2025 (7amza.it@gmail.com)
 */
@Service
@Transactional(readOnly = true)
public class FormAccessService {
    public boolean hasAnyOfPermissions(String formUid, FormPermission... requiredPermission) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
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
        return hasAnyOfPermissions(formUid, ADD_SUBMISSIONS, EDIT_SUBMISSIONS);
    }

    public boolean canViewSubmissions(String form) {
        return hasAnyOfPermissions(form, FormPermission.canViewPermissions());
    }

    public boolean canEditSubmissions(String form) {
        return hasAnyOfPermissions(form, EDIT_SUBMISSIONS);
    }

    public boolean canAddSubmissions(String form) {
        return hasAnyOfPermissions(form, ADD_SUBMISSIONS);
    }

    public boolean canApproveSubmissions(String form) {
        return hasAnyOfPermissions(form, APPROVE_SUBMISSIONS);
    }

    public boolean canDeleteSubmissions(String form) {
        return hasAnyOfPermissions(form, DELETE_SUBMISSIONS);
    }

    public Set<AssignmentFormDto> getUserForms(Set<String> assignmentFormIds, String assignmentId) {
        if (!SecurityUtils.isAuthenticated()) {
            return Set.of();
        }
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        return assignmentFormIds
            .stream()
            .filter((form) -> currentUser.getUserFormsUIDs()
                .contains(form))
            .map((form) -> AssignmentFormDto.builder()
                .form(form)
                .assignment(assignmentId)
                .canAddSubmissions(canAddSubmissions(form))
                .canEditSubmissions(canEditSubmissions(form))
                .canDeleteSubmissions(canDeleteSubmissions(form))
                .build())
            .collect(Collectors.toSet());
    }
}
