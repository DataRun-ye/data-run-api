package org.nmcpye.datarun.jpa.datasubmission.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionSavedEvent;
import org.nmcpye.datarun.jpa.datasubmission.service.SubmissionSnapshotAndPrunerService;

/**
 * @author Hamza Assada 14/08/2025 (7amza.it@gmail.com)
 */
//@Component
@Slf4j
@RequiredArgsConstructor
public class SubmissionHistoryListener {
    private final SubmissionSnapshotAndPrunerService snapshotService;

    //    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubmissionSaved(SubmissionSavedEvent event) {
        try {
            // Skip CREATE (only an update does a snapshot)
            if (event.getChangeType() != null && event
                .getChangeType().name().equalsIgnoreCase("CREATE")) {
                return;
            }

            // createSnapshot starts and commits its own tx
            snapshotService.createSnapshot(event.getSubmissionId(), event.getChangeType());

            // prune runs in REQUIRES_NEW and sees the committed snapshot
            snapshotService.pruneOldVersions(event.getSubmissionId());
        } catch (Exception e) {
            // do not throw — log and recover/monitor: we don't want to affect the main transaction or app flow
            log.error("Failed to create SubmissionHistory for event {} : {}", event, e.getMessage(), e);
        }
    }
}
