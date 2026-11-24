package org.nmcpye.datarun.etl.orchestrator;

import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.dto.EtlRunCompletion;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.etl.entity.EtlRun;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.metrics.EtlMetrics.Outcome;
import org.nmcpye.datarun.etl.service.EtlRunService;
import org.nmcpye.datarun.etl.service.EventProcessorService;
import org.nmcpye.datarun.etl.service.SubmissionNotFoundException;
import org.nmcpye.datarun.outbox.service.OutboxClaimService;
import org.nmcpye.datarun.outbox.service.OutboxProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Orchestrator that runs one ETL cycle.
 * <p>
 * Each claimed row first has its attempt counted, in a transaction of its own, then is projected in another,
 * which saves the projection and the row's success together. A failure of any kind is recorded only after that
 * transaction has rolled back, in a new one: the row is marked failed and tried again after a growing wait, or
 * dead once its tries are used up. A lost connection or an interrupt stops the batch instead, and the rows not
 * finished are released for the next run without losing an attempt.
 */
@Component
public class EtlOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EtlOrchestrator.class);

    private final EtlRunService etlRunService;
    private final OutboxClaimService outboxClaimService;
    private final OutboxProcessingService outboxProcessingService;
    private final EventProcessorService eventProcessorService;
    private final TransactionTemplate txTemplate;
    private final EtlRetryPolicy retryPolicy;
    private final EtlMetrics metrics;

    public EtlOrchestrator(EtlRunService etlRunService,
                           OutboxClaimService outboxClaimService,
                           OutboxProcessingService outboxProcessingService,
                           EventProcessorService eventProcessorService,
                           PlatformTransactionManager txManager,
                           EtlProperties properties,
                           EtlMetrics metrics) {
        this.etlRunService = etlRunService;
        this.outboxClaimService = outboxClaimService;
        this.outboxProcessingService = outboxProcessingService;
        this.eventProcessorService = eventProcessorService;
        this.txTemplate = new TransactionTemplate(txManager);
        // Statements in a row's transaction are cancelled once it has run this long.
        this.txTemplate.setTimeout((int) Math.max(1, properties.getRowTimeout().toSeconds()));
        this.retryPolicy = new EtlRetryPolicy(properties);
        this.metrics = metrics;
    }

    /**
     * Run a single processing cycle:
     * - start run -> claim batch -> for each outbox: per-outbox TX { processEvent }, failures recorded after it
     * - finish run and persist counters
     */
    public EtlRunResult runOnce(String pipelineName, int batchSize) {
        String createdBy = "etl-orchestrator";
        String codeVersion = System.getenv().getOrDefault("CODE_VERSION", "unknown");

        EtlRun run = etlRunService.startRun(pipelineName, createdBy, codeVersion);
        UUID runId = run.getEtlRunId();
        UUID ingestId = UUID.randomUUID();

        long submittedCount = 0;
        long successCount = 0;
        long failureCount = 0;
        long minSerial = -1L;
        long maxSerial = -1L;

        try {
            List<OutboxDto> claimed = outboxClaimService.claimBatchWithIngestId(ingestId, pipelineName, batchSize);
            minSerial = claimed.stream().mapToLong(OutboxDto::getSubmissionSerialNumber).min().orElse(-1L);
            maxSerial = claimed.stream().mapToLong(OutboxDto::getSubmissionSerialNumber).max().orElse(-1L);

            submittedCount = claimed.size();
            log.info("Run {} claimed {} rows (ingestId={})", runId, submittedCount, ingestId);

            int done = 0;
            boolean stopped = false;
            for (OutboxDto outbox : claimed) {
                RowResult result = Thread.currentThread().isInterrupted() ? RowResult.STOP : process(runId, outbox, ingestId);
                if (result == RowResult.STOP) {
                    stopped = true;
                    break;
                }
                done++;
                if (result == RowResult.SUCCESS) {
                    successCount++;
                } else if (result != RowResult.SKIPPED) {
                    failureCount++;
                }
            }
            if (stopped) {
                release(runId, ingestId, claimed.subList(done, claimed.size()));
            }

            // finalize run
            String status = stopped ? "stopped" : "finished";
            etlRunService.finishRun(EtlRunCompletion.builder().etlRunId(runId).status(status).submittedCount(submittedCount)
                .successCount(successCount).failureCount(failureCount).minSubmissionSerial(minSerial).maxSubmissionSerial(maxSerial)
                .build());
            log.info("Run {} {}: submitted={}, success={}, failure={}", runId, status, submittedCount, successCount, failureCount);
            return new EtlRunResult((int) submittedCount, (int) successCount, (int) failureCount, stopped);
        } catch (Exception topEx) {
            // best-effort mark run as failed
            try {
                etlRunService.finishRun(EtlRunCompletion.builder().etlRunId(runId).status("failed").submittedCount(submittedCount)
                    .successCount(successCount).failureCount(failureCount).minSubmissionSerial(minSerial).maxSubmissionSerial(maxSerial)
                    .build());
            } catch (Exception e) {
                log.error("Failed to update run status for run {}", runId, e);
            }
            log.error("Orchestration error for run {}: {}", runId, topEx.getMessage(), topEx);
            throw new RuntimeException(topEx);
        }
    }

    private enum RowResult {
        SUCCESS(Outcome.SUCCESS), FAILED(Outcome.FAILED), DEAD(Outcome.DEAD),
        /** A newer claim holds the row; that run owns it. */
        SKIPPED(null),
        /** The connection is lost or the thread interrupted: the batch stops. */
        STOP(null);

        private final Outcome outcome;

        RowResult(Outcome outcome) {
            this.outcome = outcome;
        }
    }

    private RowResult process(UUID runId, OutboxDto outbox, UUID ingestId) {
        long start = System.nanoTime();
        RowResult result = project(runId, outbox, ingestId);
        if (result.outcome != null) {
            metrics.rowProcessed(result.outcome, Duration.ofNanos(System.nanoTime() - start));
        }
        return result;
    }

    private RowResult project(UUID runId, OutboxDto outbox, UUID ingestId) {
        int attempt;
        try {
            OptionalInt started = outboxClaimService.startAttempt(outbox.getOutboxId(), ingestId);
            if (started.isEmpty()) {
                log.warn("Outbox row {} was claimed again by a newer run; leaving it to that run", outbox.getOutboxId());
                return RowResult.SKIPPED;
            }
            attempt = started.getAsInt();
        } catch (Throwable e) {
            // Not even the start could be written: nothing in this batch can be.
            log.warn("Could not start outbox row {}; the batch stops: {}", outbox.getOutboxId(), EtlFailures.describe(e));
            return RowResult.STOP;
        }
        outbox.setAttempt(attempt);
        if (retryPolicy.startedTooOften(attempt)) {
            recordDead(runId, outbox, "started " + attempt + " times without an outcome");
            return RowResult.DEAD;
        }
        try {
            txTemplate.executeWithoutResult(status -> eventProcessorService.processEvent(runId, outbox, ingestId));
            return RowResult.SUCCESS;
        } catch (SubmissionNotFoundException e) {
            // No later try can find it: the submission was removed outside the application.
            recordDead(runId, outbox, e.getMessage());
            return RowResult.DEAD;
        } catch (Throwable e) {
            // The row's transaction has rolled back: nothing of it is left to wait on.
            String error = EtlFailures.describe(e);
            if (EtlFailures.stopsTheBatch(e)) {
                log.warn("Outbox row {} met a lost connection or an interrupt on attempt {}; the batch stops: {}",
                    outbox.getOutboxId(), attempt, error);
                return RowResult.STOP;
            }
            if (log.isDebugEnabled()) {
                log.debug("Outbox row {} failed on attempt {}", outbox.getOutboxId(), attempt, EtlFailures.withoutMessages(e));
            }
            if (retryPolicy.isLastAttempt(attempt)) {
                recordDead(runId, outbox, error);
                return RowResult.DEAD;
            }
            recordRetry(runId, outbox, error, retryPolicy.delayAfter(attempt));
            return RowResult.FAILED;
        }
    }

    /**
     * Ends the lease of the rows a stopped batch did not finish, so the next run takes them at once; the row that
     * stopped it keeps the attempt it started. An interrupt is set aside for the release and then restored.
     */
    private void release(UUID runId, UUID ingestId, List<OutboxDto> rows) {
        boolean interrupted = Thread.interrupted();
        try {
            int released = outboxClaimService.release(ingestId, rows.stream().map(OutboxDto::getOutboxId).toList());
            log.warn("Run {} stopped; released {} of its claimed rows for the next run", runId, released);
        } catch (RuntimeException e) {
            log.warn("Run {} stopped and could not release its {} unfinished rows; they are claimed again when their "
                + "lease ends: {}", runId, rows.size(), EtlFailures.describe(e));
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void recordRetry(UUID runId, OutboxDto outbox, String error, Duration retryAfter) {
        log.warn("Outbox row {} failed on attempt {}, tried again in {}: {}",
            outbox.getOutboxId(), outbox.getAttempt(), retryAfter, error);
        try {
            outboxProcessingService.recordRetry(runId, outbox, error, retryAfter);
        } catch (RuntimeException e) {
            log.error("Could not record the failure of outbox row {}; it is claimed again when its lease ends",
                outbox.getOutboxId(), e);
        }
    }

    private void recordDead(UUID runId, OutboxDto outbox, String error) {
        log.error("Outbox row {} is dead after attempt {}: {}", outbox.getOutboxId(), outbox.getAttempt(), error);
        try {
            outboxProcessingService.recordDead(runId, outbox, error);
        } catch (RuntimeException e) {
            log.error("Could not record outbox row {} as dead; it is claimed again when its lease ends",
                outbox.getOutboxId(), e);
        }
    }
}
