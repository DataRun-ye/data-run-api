package org.nmcpye.datarun.jpa.datasubmission.command;

import org.nmcpye.datarun.common.exceptions.DatabaseFailures;
import org.nmcpye.datarun.common.exceptions.ErrorCodeException;
import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;

/**
 * What the submission commands write to the logs, and to a failed release's record, about a failure: identifiers and
 * shapes only. Production keeps
 * these logs in its container logs, so a line never carries a login, a submitted value, or an exception's message,
 * which can quote submitted values (a refusal names the Reference value it refused; a database or JSON error can
 * quote the row). Production's JDBC URLs also set {@code logServerErrorDetail=false}, but these lines do not rely on
 * it. A failure is described by its exception types, its SQLState when there is one, and the application frames it
 * was thrown through, which is what a diagnosis starts from.
 */
final class SubmissionLog {

    /** PostgreSQL's unique_violation. */
    private static final String UNIQUE_VIOLATION = "23505";
    /** PostgreSQL's lock_not_available: a lock wait reached the transaction's {@code lock_timeout}. */
    private static final String LOCK_NOT_AVAILABLE = "55P03";

    private SubmissionLog() {
    }

    /**
     * Whether another change held what the transaction waited for, the submission first of all, past the bound the
     * submission lock sets ({@link SubmissionLock}). The holder was stalled, or slower than the bound; the waiting
     * upload item fails and the phone sends it again, and an admin delete is refused.
     */
    static boolean isLockTimeout(Throwable failure) {
        return DatabaseFailures.sqlState(failure).filter(LOCK_NOT_AVAILABLE::equals).isPresent();
    }

    /**
     * Whether another save of the same new submission committed first: a phone re-sending a submission while its
     * first request still runs, or a held release replaying an upload while a live copy of it is saved. Both saves
     * found no row, both inserted, and the later insert hit the uid's unique constraint. That is an expected outcome,
     * not a fault: the phone sends again, and the next release marks the held copy superseded.
     */
    static boolean isUidRace(Throwable failure) {
        return DatabaseFailures.sqlState(failure).filter(UNIQUE_VIOLATION::equals).isPresent()
            && DatabaseFailures.constraint(failure).filter(DataSubmission.UID_CONSTRAINT::equals).isPresent();
    }

    /** A refusal the server means: its error code, else its type. */
    static String refusal(ErrorCodeException refusal) {
        return refusal.getErrorCode() != null ? refusal.getErrorCode().name() : refusal.getClass().getSimpleName();
    }

    /** Any failure in one word: the error code of a refusal that has one, else the exception's type. */
    static String code(Throwable failure) {
        return failure instanceof ErrorCodeException refusal ? refusal(refusal) : failure.getClass().getSimpleName();
    }

    /**
     * An unexpected failure: the exception types from the outermost to the root cause, the first SQLState among
     * them, and the root cause's first application frames ({@link FailureShape}), for example
     * {@code DataIntegrityViolationException < PSQLException, SQLState 23505, at OutboxWriter.batchInsert:67 <
     * DefaultDataSubmissionService.upsert:113}.
     */
    static String failure(Throwable failure) {
        return FailureShape.of(failure);
    }
}
