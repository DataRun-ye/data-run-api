package org.nmcpye.datarun.etl.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Keeps ETL runs from overlapping, on one instance or several. The lock is a transaction-scoped PostgreSQL
 * advisory lock, taken on one connection that is borrowed for the whole run and kept apart from the run's own
 * transactions. Ending that connection's transaction releases the lock, and so does losing the connection, so it
 * cannot be left behind on a pooled connection.
 */
@Component
public class EtlRunLock {

    private static final Logger log = LoggerFactory.getLogger(EtlRunLock.class);

    /** The advisory lock key the ETL has always used. */
    static final long KEY = 123456789012345L;

    private final DataSource dataSource;

    public EtlRunLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Runs {@code work} while holding the lock and returns its result, or returns empty at once when another run
     * holds the lock.
     */
    public <T> Optional<T> callExclusively(Supplier<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            if (autoCommit) {
                connection.setAutoCommit(false);
            }
            try {
                if (!tryLock(connection)) {
                    return Optional.empty();
                }
                return Optional.of(work.get());
            } finally {
                endTransaction(connection, autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Could not take the ETL run lock", e);
        }
    }

    private static boolean tryLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_xact_lock(?)")) {
            statement.setLong(1, KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    /** Rolls back the lock's transaction, which releases the lock; a lost connection has released it already. */
    private static void endTransaction(Connection connection, boolean autoCommit) {
        try {
            connection.rollback();
            if (autoCommit) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.warn("Could not end the ETL lock's transaction; closing its connection releases the lock", e);
        }
    }
}
