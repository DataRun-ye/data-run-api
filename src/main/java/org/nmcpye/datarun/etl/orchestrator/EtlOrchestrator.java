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
import org.nmcpye.datarun.submission.feed.publisher.SubmissionPublisher;
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
 * dead once its tries are used up. A lost connection or an interrupt also stops the batch: the row that met it is
 * recorded like any failure, with the usual retry wait, so it can neither hold the head of the queue nor die before
 * its normal schedule, and the rows not started are released for the next run without losing an attempt.
 * <p>
 * While the submission feed is on, a row's transaction also publishes the change to the feed: it takes the feed's lock
 * first, then writes tall and events, then runs the feed part in a savepoint ({@link SubmissionPublisher}). A feed part
 * that fails rolls back to its savepoint and leaves a pending change; tall, events and the row's success still commit.
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
    private final SubmissionPublisher feed;

    public EtlOrchestrator(EtlRunService etlRunService,
                           OutboxClaimService outboxClaimService,
                           OutboxProcessingService outboxProcessingService,
                           EventProcessorService eventProcessorService,
                           PlatformTransactionManager txManager,
                           EtlProperties properties,
                           EtlMetrics metrics,
                           SubmissionPublisher feed) {
        this.etlRunService = etlRunService;
        this.outboxClaimService = outboxClaimService;
        this.outboxProcessingService = outboxProcessingService;
        this.eventProcessorService = eventProcessorService;
        this.txTemplate = new TransactionTemplate(txManager);
        // Statements in a row's transaction are cancelled once it has run this long.
        this.txTemplate.setTimeout((int) Math.max(1, properties.getRowTimeout().toSeconds()));
        this.retryPolicy = new EtlRetryPolicy(properties);
        this.metrics = metrics;
        this.feed = feed;
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
                RowResult result = Thread.currentThread().isInterrupted()
                    ? RowResult.STOP_BEFORE_START : process(runId, outbox, ingestId);
                if (result == RowResult.STOP_BEFORE_START) {
                    stopped = true;
                    break;
                }
                done++;
                if (result == RowResult.SUCCESS) {
                    successCount++;
                } else if (result != RowResult.SKIPPED) {
                    failureCount++;
                }
                if (result.stopsTheBatch) {
                    stopped = true;
                    break;
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
        SUCCESS(Outcome.SUCCESS, false), FAILED(Outcome.FAILED, false), DEAD(Outcome.DEAD, false),
        /** A newer claim holds the row; that run owns it. */
        SKIPPED(null, false),
        /** The row could not be started (or the thread was interrupted first): the batch stops before it. */
        STOP_BEFORE_START(null, true),
        /** The row met a lost connection or an interrupt; it is recorded as failed and the batch stops after it. */
        FAILED_AND_STOP(Outcome.FAILED, true),
        DEAD_AND_STOP(Outcome.DEAD, true);

        private final Outcome outcome;
        private final boolean stopsTheBatch;

        RowResult(Outcome outcome, boolean stopsTheBatch) {
            this.outcome = outcome;
            this.stopsTheBatch = stopsTheBatch;
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
            return RowResult.STOP_BEFORE_START;
        }
        outbox.setAttempt(attempt);
        if (retryPolicy.startedTooOften(attempt)) {
            recordDead(runId, outbox, "started " + attempt + " times without an outcome");
            return RowResult.DEAD;
        }
        boolean publishing = feed.isOn();
        try {
            txTemplate.executeWithoutResult(status -> {
                if (publishing) {
                    feed.lockFeed();
                }
                eventProcessorService.processEvent(runId, outbox, ingestId);
                if (publishing) {
                    feed.publishChange(outbox.getSubmissionId());
                }
            });
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
                // Recorded like any failure, so the row waits its turn instead of heading the next claim.
                boolean[] dead = new boolean[1];
                withInterruptSetAside(() -> dead[0] = recordFailure(runId, outbox, attempt, error));
                return dead[0] ? RowResult.DEAD_AND_STOP : RowResult.FAILED_AND_STOP;
            }
            if (log.isDebugEnabled()) {
                log.debug("Outbox row {} failed on attempt {}", outbox.getOutboxId(), attempt, EtlFailures.withoutMessages(e));
            }
            return recordFailure(runId, outbox, attempt, error) ? RowResult.DEAD : RowResult.FAILED;
        }
    }

    /** Records a failed row: dead on its last attempt, else failed with the usual retry wait. True when dead. */
    private boolean recordFailure(UUID runId, OutboxDto outbox, int attempt, String error) {
        if (retryPolicy.isLastAttempt(attempt)) {
            recordDead(runId, outbox, error);
            return true;
        }
        recordRetry(runId, outbox, error, retryPolicy.delayAfter(attempt));
        return false;
    }

    /**
     * Ends the lease of the rows a stopped batch did not start, so the next run takes them at once; they lose no
     * attempt. The row that stopped the batch was recorded as failed, with its retry wait.
     */
    private void release(UUID runId, UUID ingestId, List<OutboxDto> rows) {
        if (rows.isEmpty()) {
            return;
        }
        withInterruptSetAside(() -> {
            try {
                int released = outboxClaimService.release(ingestId, rows.stream().map(OutboxDto::getOutboxId).toList());
                log.warn("Run {} stopped; released {} of its claimed rows for the next run", runId, released);
            } catch (RuntimeException e) {
                log.warn("Run {} stopped and could not release its {} unstarted rows; they are claimed again when "
                    + "their lease ends: {}", runId, rows.size(), EtlFailures.describe(e));
            }
        });
    }

    /** Runs a short write with the thread's interrupt set aside, so the pool hands out a connection; restores it. */
    private static void withInterruptSetAside(Runnable write) {
        boolean interrupted = Thread.interrupted();
        try {
            write.run();
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
