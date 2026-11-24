package org.nmcpye.datarun.jpa.datasubmission.command;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The one place where submissions are created, changed, deleted, held or released. Controllers adapt their routes to
 * a {@link SubmissionCommand}; every command runs one set of checks, writes each submission in a transaction of its
 * own, and writes the submission's outbox row in that same transaction.
 */
@Service
@RequiredArgsConstructor
public class SubmissionCommandService {

    private final SubmissionUploads uploads;
    private final DataSubmissionService submissionService;
    private final DataSubmissionRepository submissions;
    private final PlatformTransactionManager transactionManager;
    private final HeldSubmissionDecisions held;

    /**
     * Phone uploads on the versioned route. Each submission is checked and saved in its own transaction, or held
     * when it comes before its campaign's go-live; either way the phone is told it was saved.
     */
    public EntitySaveSummaryVM upload(SubmissionCommand.Upload command) {
        return uploads.save(command.items(), command.actor(), IntakeMode.LIVE);
    }

    /**
     * Practice uploads: every submission runs the same checks and save as an {@link #upload}, then the save is rolled
     * back and the upload held as practice. The phone is told it was saved.
     */
    public EntitySaveSummaryVM uploadPractice(SubmissionCommand.PracticeUpload command) {
        return uploads.save(command.items(), command.actor(), IntakeMode.PRACTICE);
    }

    /**
     * Uploads on the legacy route: each submission in its own transaction, with that route's own order of checks
     * (see {@link SubmissionUploads#saveLegacy}). Never held, and no Reference values are resolved.
     */
    public EntitySaveSummaryVM uploadLegacy(SubmissionCommand.LegacyUpload command) {
        return uploads.saveLegacy(command.items(), command.actor());
    }

    /**
     * An admin's delete, in one transaction: the submission is marked deleted, and its DELETE event, carrying its
     * last data, is written to the outbox. Only an admin may delete. An unknown uid fails with
     * {@link java.util.NoSuchElementException} and writes nothing.
     */
    public void delete(SubmissionCommand.Delete command) {
        if (!command.actor().admin()) {
            throw new AccessDeniedException("Only an admin may delete a submission");
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            submissionService.softDelete(submissions.findByUid(command.uid()).orElseThrow()));
    }

    /**
     * An admin's discard of held uploads: each one is labelled DISCARDED in the admin's name, only if it is unchanged
     * since it was read (see {@link HeldSubmissionDecisions#discard}).
     */
    public HeldDecisionResult discardHeld(SubmissionCommand.DiscardHeld command) {
        return held.discard(command.actor(), command.selection());
    }

    /**
     * An admin's release of held uploads: each one is replayed through the upload checks and save on behalf of its
     * original submitter, or marked superseded when its uid is already in the real data, and the decision is
     * recorded in the admin's name (see {@link HeldSubmissionDecisions#release}).
     */
    public HeldDecisionResult releaseHeld(SubmissionCommand.ReleaseHeld command) {
        return held.release(command.actor(), command.selection());
    }
}
