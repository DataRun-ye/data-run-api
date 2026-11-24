package org.nmcpye.datarun.etl.admin;

import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledOrchestratorRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledOrchestratorRunner.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final EtlOrchestrator orchestrator;
    private final EtlRunLock lock;
    private final EtlMetrics metrics;

    @Value("${migration.size:100}")
    private int SCHEDULE_BATCH_SIZE;

    public ScheduledOrchestratorRunner(NamedParameterJdbcTemplate jdbc, EtlOrchestrator orchestrator, EtlRunLock lock,
                                       EtlMetrics metrics) {
        this.jdbc = jdbc;
        this.orchestrator = orchestrator;
        this.lock = lock;
        this.metrics = metrics;
    }

    /** Whether a claim would find a row: pending or failed, and due (not leased, its retry time come). */
    private static final String CLAIMABLE_SQL = """
        SELECT count(*) FROM outbox
        WHERE status IN ('pending', 'failed')
          AND (next_attempt_at IS NULL OR next_attempt_at <= now())
        """;

    @Scheduled(cron = "${migration.cron:0 */1 * * * *}")
    public void scheduledRun() {
        log.info("Checking for pending outbox events...");
        Integer countPending = jdbc.queryForObject(CLAIMABLE_SQL, new MapSqlParameterSource(), Integer.class);
        if (countPending != null && countPending > 0) {
            log.info("{} outbox events due, starting orchestrator run ...", countPending);
            boolean ran = lock.callExclusively(() -> {
                orchestrator.runOnce("scheduled", SCHEDULE_BATCH_SIZE);
                return true;
            }).isPresent();
            if (!ran) {
                log.info("Another ETL run holds the lock; skipping this run");
                return;
            }
        } else {
            log.info("No pending outbox even");
        }
        metrics.pollSucceeded();
    }
}
