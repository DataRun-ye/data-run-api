package org.nmcpye.datarun.analytics.domaintabletoolkit.pivot;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class DdlExecutor {
    private final JdbcTemplate jdbc;
    private final Naming naming;
    @Transactional
    public void execute(String sql) {
        // single-shot execute for generated DDL; keep simple
        jdbc.execute(sql);
    }

    /**
     * Atomic swap for a template facts table.
     * We expect SqlGenerator to create table named: <baseFqName>_new
     * After validation we rename:
     * - DROP analytics.<base>_old
     * - ALTER TABLE analytics.<base> RENAME TO <base>_old
     * - ALTER TABLE analytics.<base>_new RENAME TO <base>
     * <p>
     * All operations executed on a single connection and committed atomically.
     */
    public void atomicSwapTemplate(String templateUid) {
        String baseFq = naming.fqFactTableForTemplate(templateUid); // analytics.fact_<sanitized>
        String newFq = Naming.newName(baseFq);   // analytics.fact_<sanitized>_new
        String oldFq = Naming.oldName(baseFq);   // analytics.fact_<sanitized>_old

        String dropOld = String.format("DROP TABLE IF EXISTS %s;", oldFq);
        String renameCurrentToOld = String.format("ALTER TABLE IF EXISTS %s RENAME TO %s;", baseFq, oldFq);
        String renameNewToCurrent = String.format("ALTER TABLE IF EXISTS %s RENAME TO %s;", newFq, baseFq);

        jdbc.execute((ConnectionCallback<Void>) conn -> {
            boolean prevAutoCommit;
            try {
                prevAutoCommit = conn.getAutoCommit();
            } catch (SQLException ex) {
                throw new DataAccessResourceFailureException("Failed to get connection auto-commit", ex);
            }

            try (Statement st = conn.createStatement()) {
                conn.setAutoCommit(false);
                st.execute(dropOld);
                st.execute(renameCurrentToOld);
                st.execute(renameNewToCurrent);
                conn.commit();
                return null;
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException e) { /* log if you want */ }
                throw new DataAccessResourceFailureException("atomic swap failed for template " + templateUid, ex);
            } finally {
                try {
                    conn.setAutoCommit(prevAutoCommit);
                } catch (SQLException ignored) {
                }
            }
        });
    }

    /**
     * Atomic swap for an activity facts table (and its repeats table).
     * Both swaps are executed in the same connection / transaction to keep them consistent.
     * <p>
     * baseName is "fact_<sanitized>" for activities as well; you can use a separate naming
     * convention if you prefer (e.g. fact_activity_<id>).
     */
    public void atomicSwapActivity(String activityId) {
        // you can change naming for activities if you want a different base prefix
        String sanitized = Naming.sanitize(activityId);
        String baseFq = "analytics.fact_" + sanitized;          // main facts table
        String baseFqRepeats = Naming.repeatsTable(baseFq);

        String newFq = Naming.newName(baseFq);
        String oldFq = Naming.oldName(baseFq);

        String newFqRepeats = Naming.newName(baseFqRepeats);
        String oldFqRepeats = Naming.oldName(baseFqRepeats);

        String dropOld = String.format("DROP TABLE IF EXISTS %s;", oldFq);
        String dropOldRepeats = String.format("DROP TABLE IF EXISTS %s;", oldFqRepeats);
        String renameCurrentToOld = String.format("ALTER TABLE IF EXISTS %s RENAME TO %s;", baseFq, oldFq);
        String renameCurrentToOldRepeats = String.format("ALTER TABLE IF EXISTS %s RENAME TO %s;", baseFqRepeats, oldFqRepeats);
        String renameNewToCurrent = String.format("ALTER TABLE IF EXISTS %s RENAME TO %s;", newFq, baseFq);
        String renameNewToCurrentRepeats = String.format("ALTER TABLE IF EXISTS %s RENAME TO %s;", newFqRepeats, baseFqRepeats);

        jdbc.execute((ConnectionCallback<Void>) conn -> {
            boolean prevAutoCommit;
            try {
                prevAutoCommit = conn.getAutoCommit();
            } catch (SQLException ex) {
                throw new DataAccessResourceFailureException("Failed to get connection auto-commit", ex);
            }

            try (Statement st = conn.createStatement()) {
                conn.setAutoCommit(false);

                st.execute(dropOld);
                st.execute(dropOldRepeats);

                st.execute(renameCurrentToOld);
                st.execute(renameCurrentToOldRepeats);

                st.execute(renameNewToCurrent);
                st.execute(renameNewToCurrentRepeats);

                conn.commit();
                return null;
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException e) { /* log */ }
                throw new DataAccessResourceFailureException("atomic swap failed for activity " + activityId, ex);
            } finally {
                try {
                    conn.setAutoCommit(prevAutoCommit);
                } catch (SQLException ignored) {
                }
            }
        });
    }
}

