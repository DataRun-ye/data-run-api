package org.nmcpye.datarun.jpa.datasubmission.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada 20/08/2025 (7amza.it@gmail.com)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionAccessValidator {
    private final AssignmentFormAccessService formAccessService;
    private final AssignmentRepository assignmentRepository;
    private final StoredSubmissionGuard storedSubmissionGuard;

    public DataSubmission validateAccess(DataSubmission submission, CurrentUserDetails user) {
        if (!user.isSuper()) {
            if (submission.getAssignment() == null) {
                throw new DomainValidationException("Assignment is required");
            }
            final var assignment = assignmentRepository.findByUid(submission.getAssignment())
                .orElseThrow(() -> new DomainValidationException("Assignment is required"));

            final var incomingTeam = assignment.getTeam().getUid();
            if (!AssignmentFormAccessService.isInTeamScope(user, assignment)) {
                log.error("User {}, with team {} cannot submit data", user.getUsername(), incomingTeam);
                throw new IllegalQueryException(ErrorCode.E4114, submission.getTeam(), submission.getUid());
            }
            if (!formAccessService.canSubmitData(
                user,
                assignment,
                submission.getForm())) {
                log.error(
                    "User {} cannot submit form {} for assignment {}",
                    user.getUsername(),
                    submission.getForm(),
                    submission.getAssignment());
                throw new IllegalQueryException(
                    ErrorCode.E4122,
                    submission.getForm(),
                    submission.getAssignment());
            }
            storedSubmissionGuard.check(submission, user);
        }

        return submission;
    }
}
