package org.nmcpye.datarun.jpa.datasubmission.command;

import org.nmcpye.datarun.common.exceptions.ErrorCodeException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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

    private static final String APPLICATION = "org.nmcpye.datarun.";
    private static final int FRAMES = 3;

    private SubmissionLog() {
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
     * them, and the root cause's first application frames, for example
     * {@code DataIntegrityViolationException < PSQLException, SQLState 23505, at OutboxWriter.batchInsert:67 <
     * DefaultDataSubmissionService.upsertAll:149}.
     */
    static String failure(Throwable failure) {
        List<String> types = new ArrayList<>();
        String sqlState = null;
        Throwable root = failure;
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            types.add(cause.getClass().getSimpleName());
            if (sqlState == null && cause instanceof SQLException sql) {
                sqlState = sql.getSQLState();
            }
            root = cause;
        }
        StringBuilder shape = new StringBuilder(String.join(" < ", types));
        if (sqlState != null) {
            shape.append(", SQLState ").append(sqlState);
        }
        List<String> frames = applicationFrames(root);
        if (frames.isEmpty() && root != failure) {
            frames = applicationFrames(failure);
        }
        if (!frames.isEmpty()) {
            shape.append(", at ").append(String.join(" < ", frames));
        }
        return shape.toString();
    }

    private static List<String> applicationFrames(Throwable thrown) {
        List<String> frames = new ArrayList<>();
        for (StackTraceElement frame : thrown.getStackTrace()) {
            if (frames.size() == FRAMES) {
                break;
            }
            String type = frame.getClassName();
            if (type.startsWith(APPLICATION) && !type.contains("$$")) {
                frames.add(type.substring(type.lastIndexOf('.') + 1) + "." + frame.getMethodName()
                    + ":" + frame.getLineNumber());
            }
        }
        return frames;
    }
}
