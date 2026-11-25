package org.nmcpye.datarun.jpa.datasubmission.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The one place where submissions are created, changed, deleted, held or released. Controllers adapt their routes to
 * a {@link SubmissionCommand}; every command runs one set of checks, writes each submission in a transaction of its
 * own, and records each change in that same transaction: a revision naming the command and who acted
 * ({@link RevisionContext}), and the outbox row that announces it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionCommandService {

    private final SubmissionUploads uploads;
    private final DataSubmissionService submissionService;
    private final SubmissionLock submissionLock;
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

    /** What an admin delete found. */
    public enum DeleteOutcome {
        DELETED,
        ALREADY_DELETED,
        /** No submission has that uid; nothing was written. */
        NOT_FOUND
    }

    /**
     * An admin's delete, in one transaction: the submission is marked deleted, and its DELETE event, carrying its
     * last data, is written to the outbox. A submission already deleted is left as it is, with no second event and
     * its first delete time, the rule an upload's {@code deleted=true} already follows. Only an admin may delete.
     * The transaction takes the submission lock first ({@link SubmissionLock}), so another change of the submission
     * waits until the delete ends.
     *
     * @throws ConcurrencyFailureException when another change held the submission past the lock's bound: nothing was
     *                                     written, and the route answers 409 {@code error.concurrencyFailure}
     */
    public DeleteOutcome delete(SubmissionCommand.Delete command) {
        requireAdmin(command, "Only an admin may delete a submission");
        try {
            return new TransactionTemplate(transactionManager).execute(status -> {
                DataSubmission submission = submissionLock.take(command.uid());
                if (submission == null) {
                    return DeleteOutcome.NOT_FOUND;
                }
                if (Boolean.TRUE.equals(submission.getDeleted())) {
                    return DeleteOutcome.ALREADY_DELETED;
                }
                submissionService.softDelete(submission, RevisionContext.adminDelete(command.actor().userId()));
                return DeleteOutcome.DELETED;
            });
        } catch (RuntimeException failure) {
            if (!SubmissionLog.isLockTimeout(failure)) {
                throw failure;
            }
            // Identifiers and shapes only (SubmissionLog).
            log.warn("Submission {} not deleted by user {}: another change held it past the submission lock's "
                + "bound: {}", command.uid(), command.actor().userId(), SubmissionLog.failure(failure));
            throw failure instanceof ConcurrencyFailureException concurrency ? concurrency
                : new CannotAcquireLockException("Another change held the submission past the lock's bound", failure);
        }
    }

    /**
     * An admin's discard of held uploads: each one is labelled DISCARDED in the admin's name, only if it is unchanged
     * since it was read (see {@link HeldSubmissionDecisions#discard}). Only an admin may discard.
     */
    public HeldDecisionResult discardHeld(SubmissionCommand.DiscardHeld command) {
        requireAdmin(command, "Only an admin may discard held uploads");
        return held.discard(command.actor(), command.selection());
    }

    /**
     * An admin's release of held uploads: each one is replayed through the upload checks and save on behalf of its
     * original submitter, or marked superseded when its uid is already in the real data, and the decision is
     * recorded in the admin's name (see {@link HeldSubmissionDecisions#release}). Only an admin may release.
     */
    public HeldDecisionResult releaseHeld(SubmissionCommand.ReleaseHeld command) {
        requireAdmin(command, "Only an admin may release held uploads");
        return held.release(command.actor(), command.selection());
    }

    /** The command's own rule, kept here as well as on its route: this service is what every caller goes through. */
    private static void requireAdmin(SubmissionCommand command, String refusal) {
        if (!command.actor().admin()) {
            throw new AccessDeniedException(refusal);
        }
    }
}
