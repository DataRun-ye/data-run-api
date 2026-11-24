package org.nmcpye.datarun.etl.orchestrator;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;

/**
 * Describes why an outbox row failed, in a form that may be stored and logged without submitted values. The JDBC
 * driver's batch errors repeat the failed statement with its bound values, and other messages (number parsing,
 * JSON parsing) can quote an input. So a description keeps only the database's own error line, whose primary
 * message names types, columns and constraints (the ETL binds typed values and casts no submitted text), or else
 * the root cause's type and where it was thrown, never its message.
 */
final class EtlFailures {

    static final int MAX_LENGTH = 1000;

    /** Deep enough for any real cause chain; guards against a cycle among causes. */
    private static final int MAX_CAUSES = 20;

    private static final String APPLICATION_PACKAGE = "org.nmcpye.";

    private EtlFailures() {
    }

    static String describe(Throwable failure) {
        SQLException sql = deepestSqlException(failure);
        String text;
        if (sql != null) {
            String state = sql.getSQLState() == null ? "" : " [SQLState " + sql.getSQLState() + "]";
            text = firstLine(sql.getMessage()) + state;
        } else {
            Throwable root = rootCause(failure);
            text = root.getClass().getSimpleName() + where(root, failure);
        }
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }

    /**
     * Whether the failure says nothing about the row: the database connection is lost or refused (SQLState class
     * 08, or the driver's or pool's connection exceptions), the database is shutting down (57P01 to 57P03), or the
     * thread was interrupted. Every following row would meet the same, so the batch stops.
     */
    static boolean stopsTheBatch(Throwable failure) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        int depth = 0;
        for (Throwable t = failure; t != null && depth++ < MAX_CAUSES; t = next(t)) {
            if (t instanceof InterruptedException || t instanceof InterruptedIOException
                || t instanceof ClosedByInterruptException
                || t instanceof SQLTransientConnectionException || t instanceof SQLNonTransientConnectionException
                || t instanceof SQLRecoverableException) {
                return true;
            }
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                String state = sql.getSQLState();
                if (state.startsWith("08") || state.equals("57P01") || state.equals("57P02") || state.equals("57P03")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The failure's exception types and stack frames with every message left out, for a debug log that keeps the
     * frames a developer needs and none of the values a message may repeat.
     */
    static Throwable withoutMessages(Throwable failure) {
        Throwable head = null;
        Throwable tail = null;
        int depth = 0;
        for (Throwable t = failure; t != null && depth++ < MAX_CAUSES; t = next(t)) {
            Throwable copy = new MessageLeftOut(t);
            if (head == null) {
                head = copy;
            } else {
                tail.initCause(copy);
            }
            tail = copy;
        }
        return head;
    }

    private static final class MessageLeftOut extends Exception {
        MessageLeftOut(Throwable original) {
            super(original.getClass().getName() + " (message left out)");
            setStackTrace(original.getStackTrace());
        }
    }

    private static Throwable next(Throwable t) {
        return t.getCause() == t ? null : t.getCause();
    }

    private static SQLException deepestSqlException(Throwable failure) {
        SQLException deepest = null;
        int depth = 0;
        for (Throwable t = failure; t != null && depth++ < MAX_CAUSES; t = next(t)) {
            if (t instanceof SQLException sql) {
                deepest = sql;
            }
        }
        // A batch error's message names the statement and its values; the error it wraps carries the cause.
        depth = 0;
        while (deepest instanceof BatchUpdateException batch && batch.getNextException() != null && depth++ < MAX_CAUSES) {
            deepest = batch.getNextException();
        }
        return deepest;
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable root = failure;
        int depth = 0;
        while (next(root) != null && depth++ < MAX_CAUSES) {
            root = next(root);
        }
        return root;
    }

    /** " at" the frame that threw, and ", from" the first frame in the application's code when that differs. */
    private static String where(Throwable root, Throwable failure) {
        StackTraceElement[] frames = root.getStackTrace();
        if (frames.length == 0) {
            return "";
        }
        StackTraceElement application = firstApplicationFrame(root);
        if (application == null) {
            application = firstApplicationFrame(failure);
        }
        return " at " + frames[0] + (application == null || application.equals(frames[0]) ? "" : ", from " + application);
    }

    private static StackTraceElement firstApplicationFrame(Throwable t) {
        for (StackTraceElement frame : t.getStackTrace()) {
            if (frame.getClassName().startsWith(APPLICATION_PACKAGE)) {
                return frame;
            }
        }
        return null;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "(no message)";
        }
        int end = message.indexOf('\n');
        return (end < 0 ? message : message.substring(0, end)).trim();
    }
}
