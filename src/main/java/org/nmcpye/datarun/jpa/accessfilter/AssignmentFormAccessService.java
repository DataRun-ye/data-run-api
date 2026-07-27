package org.nmcpye.datarun.jpa.accessfilter;

import org.apache.commons.collections4.CollectionUtils;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentFormDto;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.nmcpye.datarun.common.enumeration.FormPermission.ADD_SUBMISSIONS;
import static org.nmcpye.datarun.common.enumeration.FormPermission.DELETE_SUBMISSIONS;
import static org.nmcpye.datarun.common.enumeration.FormPermission.EDIT_SUBMISSIONS;

/**
 * @author Hamza Assada 25/04/2025 (7amza.it@gmail.com)
 */
@Service
@Transactional(readOnly = true)
public class AssignmentFormAccessService {

    public boolean canSubmitData(
        CurrentUserDetails user,
        Assignment assignment,
        String formUid
    ) {
        return hasAnyPermission(
            user,
            assignment,
            formUid,
            ADD_SUBMISSIONS,
            EDIT_SUBMISSIONS);
    }

    public boolean canAddSubmissions(
        CurrentUserDetails user,
        Assignment assignment,
        String formUid
    ) {
        return hasAnyPermission(user, assignment, formUid, ADD_SUBMISSIONS);
    }

    public Set<AssignmentFormDto> getAccessibleForms(
        Assignment assignment,
        CurrentUserDetails user
    ) {
        if (user == null || assignment == null || assignment.getTeam() == null
            || assignment.getForms() == null) {
            return Set.of();
        }

        return assignment.getForms().stream()
            .filter(form -> hasAnyAccess(user, assignment, form))
            .map(form -> AssignmentFormDto.builder()
                .form(form)
                .assignment(assignment.getUid())
                .canAddSubmissions(
                    hasAnyPermission(user, assignment, form, ADD_SUBMISSIONS))
                .canEditSubmissions(
                    hasAnyPermission(user, assignment, form, EDIT_SUBMISSIONS))
                .canDeleteSubmissions(
                    hasAnyPermission(user, assignment, form, DELETE_SUBMISSIONS))
                .build())
            .collect(Collectors.toSet());
    }

    private boolean hasAnyAccess(
        CurrentUserDetails user,
        Assignment assignment,
        String formUid
    ) {
        if (user.isSuper()) {
            return true;
        }
        return matchingAccess(user, assignment, formUid).findAny().isPresent();
    }

    private boolean hasAnyPermission(
        CurrentUserDetails user,
        Assignment assignment,
        String formUid,
        FormPermission... requiredPermissions
    ) {
        if (user == null || assignment == null || assignment.getTeam() == null
            || assignment.getForms() == null
            || !assignment.getForms().contains(formUid)) {
            return false;
        }
        if (user.isSuper()) {
            return true;
        }

        return matchingAccess(user, assignment, formUid)
            .anyMatch(access -> CollectionUtils.containsAny(
                access.getPermissions(),
                requiredPermissions));
    }

    private Stream<UserFormAccess> matchingAccess(
        CurrentUserDetails user,
        Assignment assignment,
        String formUid
    ) {
        List<UserFormAccess> access = user.getFormAccess() == null
            ? List.of()
            : user.getFormAccess();
        String teamUid = assignment.getTeam().getUid();
        Instant now = Instant.now();
        return access.stream()
            .filter(entry -> Objects.equals(entry.getTeam(), teamUid))
            .filter(entry -> Objects.equals(entry.getForm(), formUid))
            .filter(entry -> entry.getPermissions() != null)
            .filter(entry ->
                (entry.getValidFrom() == null || entry.getValidFrom().isBefore(now))
                    && (entry.getValidTo() == null || entry.getValidTo().isAfter(now)));
    }
}
