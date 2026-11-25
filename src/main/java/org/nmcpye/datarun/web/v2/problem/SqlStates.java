package org.nmcpye.datarun.web.v2.problem;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * PostgreSQL error states read from an exception's causes. Spring's default JDBC translation leaves some states
 * uncategorized (a lock timeout through JdbcTemplate is an {@code UncategorizedSQLException}, while Hibernate
 * reports the same state as a pessimistic locking failure), so v2 decides on the state itself.
 */
public final class SqlStates {

    /** {@code lock_not_available}: a lock wait reached {@code lock_timeout}. */
    public static final String LOCK_NOT_AVAILABLE = "55P03";

    private SqlStates() {
    }

    /** Whether any cause is an {@link SQLException} with this state. */
    public static boolean has(Throwable failure, String sqlState) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                for (SQLException next = sql; next != null; next = next.getNextException()) {
                    if (sqlState.equals(next.getSQLState())) {
                        return true;
                    }
                    if (next.getNextException() == next) {
                        break;
                    }
                }
            }
        }
        return false;
    }
}
