package org.nmcpye.datarun.etl.orchestrator;

import java.sql.BatchUpdateException;
import java.sql.SQLException;

/**
 * Describes why an outbox row failed, in a form that may be stored and logged. The JDBC driver's batch errors
 * repeat the failed statement with its bound values, which are submitted data; the description keeps only the
 * database's own error line, or the root cause's type and first line.
 */
final class EtlFailures {

    static final int MAX_LENGTH = 1000;

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
            text = root.getClass().getSimpleName() + ": " + firstLine(root.getMessage());
        }
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }

    private static SQLException deepestSqlException(Throwable failure) {
        SQLException deepest = null;
        for (Throwable t = failure; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof SQLException sql) {
                deepest = sql;
            }
        }
        // A batch error's message names the statement and its values; the error it wraps carries the cause.
        while (deepest instanceof BatchUpdateException batch && batch.getNextException() != null) {
            deepest = batch.getNextException();
        }
        return deepest;
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "(no message)";
        }
        int end = message.indexOf('\n');
        return (end < 0 ? message : message.substring(0, end)).trim();
    }
}
