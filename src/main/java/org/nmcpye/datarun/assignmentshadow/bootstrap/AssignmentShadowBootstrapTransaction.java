package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class AssignmentShadowBootstrapTransaction {

    static final long ADVISORY_LOCK_KEY = 2_180_049_398_680_906_053L;

    private final JdbcTemplate jdbc;
    private final AssignmentShadowBaselineSnapshot snapshot;
    private final AssignmentShadowBootstrapStore store;
    private final AssignmentShadowComparison comparison;

    public AssignmentShadowBootstrapTransaction(
        JdbcTemplate jdbc,
        AssignmentShadowBaselineSnapshot snapshot,
        AssignmentShadowBootstrapStore store,
        AssignmentShadowComparison comparison
    ) {
        this.jdbc = jdbc;
        this.snapshot = snapshot;
        this.store = store;
        this.comparison = comparison;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AssignmentShadowBootstrapReport runOnce() {
        jdbc.execute("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK_KEY + ")");
        assertTransactionIsolation();

        AssignmentShadowBootstrapMetrics metrics = new AssignmentShadowBootstrapMetrics();
        snapshot.stage(metrics);
        store.persistAndVerify(metrics, Instant.now());

        AssignmentShadowBootstrapReport report = comparison.compare(metrics);
        if (!report.successful()) {
            throw new AssignmentShadowBootstrapMismatchException(report);
        }
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
