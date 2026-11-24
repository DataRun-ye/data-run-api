package org.nmcpye.datarun.etl.admin;

import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.etl.orchestrator.EtlRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Polls the outbox for the ETL: {@code datarun.etl.poll-delay} after one poll ends, the next one starts. A poll
 * that finds a row due takes the run lock and claims batches of {@code datarun.etl.batch-size} until a batch
 * comes back short, a batch stops on a lost connection or an interrupt, or the poll has run for
 * {@code datarun.etl.max-poll-duration}. Scheduling is enabled outside the test profiles;
 * {@code datarun.etl.enabled=false} stops this instance from polling.
 */
@Service
public class ScheduledOrchestratorRunner implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledOrchestratorRunner.class);

    static final String PIPELINE = "scheduled";

    /** Whether a claim would find a row: pending or failed, and due (not leased, its retry time come). */
    private static final String DUE_SQL = """
        SELECT EXISTS (
          SELECT 1 FROM outbox
          WHERE status IN ('pending', 'failed')
            AND (next_attempt_at IS NULL OR next_attempt_at <= now()))
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final EtlOrchestrator orchestrator;
    private final EtlRunLock lock;
    private final EtlMetrics metrics;
    private final EtlProperties properties;

    public ScheduledOrchestratorRunner(NamedParameterJdbcTemplate jdbc, EtlOrchestrator orchestrator, EtlRunLock lock,
                                       EtlMetrics metrics, EtlProperties properties) {
        this.jdbc = jdbc;
        this.orchestrator = orchestrator;
        this.lock = lock;
        this.metrics = metrics;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        if (!properties.isEnabled()) {
            log.warn("The ETL does not poll the outbox on this instance (datarun.etl.enabled=false)");
            return;
        }
        Duration delay = properties.getPollDelay();
        registrar.addFixedDelayTask(new FixedDelayTask(this::scheduledRun, delay, delay));
        log.info("The ETL polls the outbox {} after each poll, {} rows a batch, for at most {} a poll",
            delay, properties.getBatchSize(), properties.getMaxPollDuration());
    }

    /** One poll. */
    public void scheduledRun() {
        if (Boolean.TRUE.equals(jdbc.queryForObject(DUE_SQL, Map.of(), Boolean.class))) {
            Optional<Integer> claimed = lock.callExclusively(this::drain);
            if (claimed.isEmpty()) {
                log.info("Another ETL run holds the lock; skipping this poll");
                return;
            }
        }
        metrics.pollSucceeded();
    }

    private int drain() {
        int batchSize = Math.max(1, properties.getBatchSize());
        long deadline = System.nanoTime() + properties.getMaxPollDuration().toNanos();
        int claimed = 0;
        EtlRunResult batch;
        do {
            batch = orchestrator.runOnce(PIPELINE, batchSize);
            claimed += batch.claimed();
        } while (!batch.stopped() && batch.claimed() >= batchSize && System.nanoTime() - deadline < 0);
        return claimed;
    }
}
