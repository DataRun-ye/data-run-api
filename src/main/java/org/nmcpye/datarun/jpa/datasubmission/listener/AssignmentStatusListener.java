package org.nmcpye.datarun.jpa.datasubmission.listener;

import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionSavedEvent;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author Hamza Assada 14/08/2025 (7amza.it@gmail.com)
 */
//@Component
public class AssignmentStatusListener {

    private final Logger log = LoggerFactory.getLogger(AssignmentStatusListener.class);

    private final DataSubmissionRepository submissionRepo;
    private final AssignmentService assignmentService;

    public AssignmentStatusListener(
        DataSubmissionRepository submissionRepo,
        AssignmentService assignmentService) {
        this.submissionRepo = submissionRepo;
        this.assignmentService = assignmentService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionSaved(SubmissionSavedEvent event) {
        try {
            String submissionId = event.getSubmissionId();
            // Re-load committed entity from DB (ensures we snapshot committed state)
            DataSubmission committed = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Committed DataSubmission not found: " + submissionId));

            assignmentService.updateStatusForSubmission(event.getSubmissionId());

            log.debug("Updated assignment status for submissionId={} version={}",
                committed.getId(), committed.getLockVersion());
        } catch (Exception e) {
            // Very important: do not throw checked exceptions that could bubble up to transaction management here.
            // Log and surface asynchronously if needed. You can also collect failed snapshots for reprocessing.
            log.error("Failed to create SubmissionHistory for event {} : {}",
                event, e.getMessage(), e);
        }
    }
}
