package org.nmcpye.datarun.etl.admin;

import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.etl.orchestrator.EtlRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Polls the outbox for the ETL: {@code datarun.etl.poll-delay} after one poll ends, the next one starts. A poll
 * that finds a row due takes the run lock and claims batches of {@code datarun.etl.batch-size} until a batch
 * comes back short, a batch stops on a lost connection or an interrupt, or the poll has run for
 * {@code datarun.etl.max-poll-duration}. A poll that stops or fails is not a success, and the pause before the next
 * one doubles, up to {@code datarun.etl.max-poll-delay}, until a poll succeeds. Scheduling is enabled outside the
 * test profiles; {@code datarun.etl.enabled=false} stops this instance from polling.
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
    private final AtomicReference<Duration> pause = new AtomicReference<>();

    private enum PollOutcome { SUCCEEDED, STOPPED, SKIPPED }

    public ScheduledOrchestratorRunner(NamedParameterJdbcTemplate jdbc, EtlOrchestrator orchestrator, EtlRunLock lock,
                                       EtlMetrics metrics, EtlProperties properties) {
        this.jdbc = jdbc;
        this.orchestrator = orchestrator;
        this.lock = lock;
        this.metrics = metrics;
        this.properties = properties;
        this.pause.set(properties.getPollDelay());
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        if (!properties.isEnabled()) {
            log.warn("The ETL does not poll the outbox on this instance (datarun.etl.enabled=false)");
            return;
        }
        registrar.addTriggerTask(this::scheduledRun, this::nextPoll);
        log.info("The ETL polls the outbox {} after each poll, {} rows a batch, for at most {} a poll",
            properties.getPollDelay(), properties.getBatchSize(), properties.getMaxPollDuration());
    }

    /** When the next poll starts: the current pause after the last poll ended, or after start-up. */
    Instant nextPoll(TriggerContext context) {
        Instant last = context.lastCompletion();
        return (last != null ? last : context.getClock().instant()).plus(pause.get());
    }

    /** One poll. */
    public void scheduledRun() {
        PollOutcome outcome;
        try {
            outcome = poll();
        } catch (RuntimeException e) {
            metrics.pollFailed();
            lengthenPause();
            throw e;
        }
        if (outcome == PollOutcome.STOPPED) {
            metrics.pollStopped();
            lengthenPause();
        } else if (outcome == PollOutcome.SUCCEEDED) {
            metrics.pollSucceeded();
            pause.set(properties.getPollDelay());
        }
    }

    private PollOutcome poll() {
        if (!Boolean.TRUE.equals(jdbc.queryForObject(DUE_SQL, Map.of(), Boolean.class))) {
            return PollOutcome.SUCCEEDED;
        }
        Optional<Boolean> stopped = lock.callExclusively(this::drain);
        if (stopped.isEmpty()) {
            log.info("Another ETL run holds the lock; skipping this poll");
            return PollOutcome.SKIPPED;
        }
        return stopped.get() ? PollOutcome.STOPPED : PollOutcome.SUCCEEDED;
    }

    private void lengthenPause() {
        Duration longest = properties.getMaxPollDelay().compareTo(properties.getPollDelay()) > 0
            ? properties.getMaxPollDelay() : properties.getPollDelay();
        Duration next = pause.updateAndGet(current -> {
            Duration doubled = current.multipliedBy(2);
            return doubled.compareTo(longest) > 0 ? longest : doubled;
        });
        log.warn("The next ETL poll waits {}", next);
    }

    /** Claims batches; true when one stopped on a lost connection or an interrupt. */
    private boolean drain() {
        int batchSize = Math.max(1, properties.getBatchSize());
        long deadline = System.nanoTime() + properties.getMaxPollDuration().toNanos();
        EtlRunResult batch;
        do {
            batch = orchestrator.runOnce(PIPELINE, batchSize);
        } while (!batch.stopped() && batch.claimed() >= batchSize && System.nanoTime() - deadline < 0);
        return batch.stopped();
    }
}
