package org.nmcpye.datarun.jpa.datasubmission.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionSavedEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author Hamza Assada 14/08/2025 (7amza.it@gmail.com)
 */

//@Component
@Slf4j
@RequiredArgsConstructor
public class AssignmentSubmissionListener {

    private final AssignmentService assignmentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionSaved(SubmissionSavedEvent event) {
        try {
            assignmentService.updateStatusForSubmission(event.getSubmissionId());
            log.debug("Updated assignment status for submissionId={}", event.getSubmissionId());
        } catch (Exception e) {
            log.error("Failed to update assignment status for event {} : {}", event, e.getMessage(), e);
        }
    }
}
