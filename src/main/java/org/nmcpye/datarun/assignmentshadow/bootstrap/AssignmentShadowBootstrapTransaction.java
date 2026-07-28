package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.nmcpye.datarun.assignmentshadow.AssignmentAuthorityCommandService;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowCheckpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class AssignmentShadowBootstrapTransaction {

    static final long ADVISORY_LOCK_KEY = AssignmentAuthorityCommandService.ADVISORY_LOCK_KEY;

    private final JdbcTemplate jdbc;
    private final AssignmentShadowBaselineSnapshot snapshot;
    private final AssignmentShadowBootstrapStore store;
    private final AssignmentShadowComparison comparison;
    private final AssignmentShadowCheckpoint checkpoint;

    public AssignmentShadowBootstrapTransaction(
        JdbcTemplate jdbc,
        AssignmentShadowBaselineSnapshot snapshot,
        AssignmentShadowBootstrapStore store,
        AssignmentShadowComparison comparison,
        AssignmentShadowCheckpoint checkpoint
    ) {
        this.jdbc = jdbc;
        this.snapshot = snapshot;
        this.store = store;
        this.comparison = comparison;
        this.checkpoint = checkpoint;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AssignmentShadowBootstrapReport runOnce() {
        jdbc.execute("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK_KEY + ")");
        assertTransactionIsolation();

        AssignmentShadowBootstrapMetrics metrics = new AssignmentShadowBootstrapMetrics();
        snapshot.stage(metrics);
        if (checkpoint.existsAndIsExact()) {
            store.measureCurrent(metrics);
            AssignmentShadowBootstrapReport currentReport = comparison.compareCurrent(metrics);
            if (!currentReport.successful()) {
                throw new AssignmentShadowBootstrapMismatchException(currentReport);
            }
            return currentReport;
        }

        Instant recordedAt = Instant.now();
        store.persistAndVerify(metrics, recordedAt);

        AssignmentShadowBootstrapReport report = comparison.compare(metrics);
        if (!report.successful()) {
            throw new AssignmentShadowBootstrapMismatchException(report);
        }
        checkpoint.recordCompleted(recordedAt);
        return report;
    }

    private void assertTransactionIsolation() {
        String isolation = jdbc.queryForObject("SHOW transaction_isolation", String.class);
        if (!"repeatable read".equals(isolation)) {
            throw new AssignmentShadowBootstrapConflictException(
                "Bootstrap requires REPEATABLE_READ but transaction isolation is " + isolation
            );
        }
    }
}
