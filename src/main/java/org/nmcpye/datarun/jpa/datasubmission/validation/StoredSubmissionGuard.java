package org.nmcpye.datarun.jpa.datasubmission.validation;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
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
 * can still be moved by someone allowed on both places); deleting needs the
 * delete permission there; and a record another user made (its creator is the
 * login that first sent it) needs the "from users" edit or delete permission.
 * Admins are not checked, as on the upload itself.</p>
 */
@Component
@RequiredArgsConstructor
public class StoredSubmissionGuard {
    private final AssignmentRepository assignments;
    private final AssignmentFormAccessService formAccess;

    /**
     * @param stored the stored submission with the incoming uid, as the upload's transaction took it under the
     *               submission lock, or null when there is none: a new submission is not checked here
     */
    public void check(DataSubmission incoming, DataSubmission stored, CurrentUserDetails user) {
        if (user.isSuper() || stored == null) {
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
        boolean madeByOthers = !madeBy(stored, user);
        if (Boolean.TRUE.equals(incoming.getDeleted())) {
            boolean mayDelete = madeByOthers
                ? formAccess.canDeleteOthersSubmissions(user, storedAssignment, stored.getForm())
                : formAccess.canDeleteSubmissions(user, storedAssignment, stored.getForm());
            if (!mayDelete) {
                throw new IllegalQueryException(ErrorCode.E4125, incoming.getUid());
            }
        } else if (madeByOthers
            && !formAccess.canEditOthersSubmissions(user, storedAssignment, stored.getForm())) {
            throw new IllegalQueryException(ErrorCode.E4126, incoming.getUid());
        }
    }

    /** A record whose creator is unknown counts as another user's. */
    private static boolean madeBy(DataSubmission stored, CurrentUserDetails user) {
        return stored.getCreatedBy() != null
            && user.getUsername() != null
            && stored.getCreatedBy().equalsIgnoreCase(user.getUsername());
    }
}
