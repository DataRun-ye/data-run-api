package org.nmcpye.datarun.jpa.datasubmission.validation;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * An upload that names an existing submission is checked against the stored
 * record, not only against what the upload says.
 *
 * <p>The callers have already checked that the user may submit the incoming
 * form on the incoming assignment. Here: a record never changes form; the user
 * must also be allowed on the stored record's assignment and form (so a record
 * can still be moved by someone allowed on both places); and deleting needs the
 * delete permission there. Admins are not checked, as on the upload itself.</p>
 */
@Component
@RequiredArgsConstructor
public class StoredSubmissionGuard {
    private final DataSubmissionRepository submissions;
    private final AssignmentRepository assignments;
    private final AssignmentFormAccessService formAccess;

    public void check(DataSubmission incoming, CurrentUserDetails user) {
        if (user.isSuper() || incoming.getUid() == null) {
            return;
        }
        DataSubmission stored = submissions.findByUid(incoming.getUid()).orElse(null);
        if (stored == null) {
            return;
        }
        if (!Objects.equals(stored.getForm(), incoming.getForm())) {
            throw new IllegalQueryException(ErrorCode.E4123, incoming.getUid(), stored.getForm());
        }
        Assignment storedAssignment = stored.getAssignment() == null
            ? null
            : assignments.findByUid(stored.getAssignment()).orElse(null);
        if (storedAssignment == null
            || !AssignmentFormAccessService.isInTeamScope(user, storedAssignment)
            || !formAccess.canSubmitData(user, storedAssignment, stored.getForm())) {
            throw new IllegalQueryException(ErrorCode.E4124, incoming.getUid());
        }
        if (Boolean.TRUE.equals(incoming.getDeleted())
            && !formAccess.canDeleteSubmissions(user, storedAssignment, stored.getForm())) {
            throw new IllegalQueryException(ErrorCode.E4125, incoming.getUid());
        }
    }
}
