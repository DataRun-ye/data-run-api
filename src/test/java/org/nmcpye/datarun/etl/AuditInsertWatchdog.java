package org.nmcpye.datarun.etl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Watches PostgreSQL for a session stuck on another transaction's uncommitted row while writing an
 * {@code outbox_processing} audit row, and cancels it once it has waited long enough to be called stuck.
 * Each cancel records how long the session waited and the state of the session it waited for.
 */
final class AuditInsertWatchdog implements AutoCloseable {

    record Wait(double seconds, String blockerState) {
    }

    private static final String STUCK = """
        SELECT a.pid,
               extract(epoch FROM now() - a.state_change) AS waited,
               (SELECT b.state FROM pg_stat_activity b WHERE b.pid = ANY (pg_blocking_pids(a.pid)) LIMIT 1) AS blocker
        FROM pg_stat_activity a
        WHERE a.datname = current_database()
          AND a.wait_event_type = 'Lock' AND a.wait_event = 'transactionid'
          AND a.query ILIKE '%outbox_processing%'
          AND a.state_change < now() - make_interval(secs => ?)
        """;

    private final Connection connection;
    private final double stuckAfterSeconds;
    private final List<Wait> cancelled = new ArrayList<>();
    private final Thread thread;
    private volatile boolean running = true;

    AuditInsertWatchdog(DataSource dataSource, double stuckAfterSeconds) throws SQLException {
        this.connection = dataSource.getConnection();
        this.connection.setAutoCommit(true);
        this.stuckAfterSeconds = stuckAfterSeconds;
        this.thread = new Thread(this::watch, "audit-insert-watchdog");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private void watch() {
        while (running) {
            try (PreparedStatement select = connection.prepareStatement(STUCK)) {
                select.setDouble(1, stuckAfterSeconds);
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        Wait wait = new Wait(rs.getDouble("waited"), rs.getString("blocker"));
                        try (PreparedStatement cancel = connection.prepareStatement("SELECT pg_cancel_backend(?)")) {
                            cancel.setInt(1, rs.getInt("pid"));
                            cancel.execute();
                        }
                        synchronized (cancelled) {
                            cancelled.add(wait);
                        }
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (SQLException e) {
                if (running) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    List<Wait> cancelled() {
        synchronized (cancelled) {
            return List.copyOf(cancelled);
        }
    }

    @Override
    public void close() throws Exception {
        running = false;
        thread.join(2000);
        connection.close();
    }
}
