package org.nmcpye.datarun.etl.orchestrator;

import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.dto.EtlRunCompletion;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.etl.entity.EtlRun;
import org.nmcpye.datarun.etl.service.EtlRunService;
import org.nmcpye.datarun.etl.service.EventProcessorService;
import org.nmcpye.datarun.outbox.service.OutboxClaimService;
import org.nmcpye.datarun.outbox.service.OutboxProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrator that runs one ETL cycle.
 * <p>
 * Each claimed row is projected in a transaction of its own, which saves the projection and the row's success
 * together. A failure is recorded only after that transaction has rolled back, in a new one: the row is marked
 * failed and tried again after a growing wait, or dead once its tries are used up.
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

    public EtlOrchestrator(EtlRunService etlRunService,
                           OutboxClaimService outboxClaimService,
                           OutboxProcessingService outboxProcessingService,
                           EventProcessorService eventProcessorService,
                           PlatformTransactionManager txManager,
                           EtlProperties properties) {
        this.etlRunService = etlRunService;
        this.outboxClaimService = outboxClaimService;
        this.outboxProcessingService = outboxProcessingService;
        this.eventProcessorService = eventProcessorService;
        this.txTemplate = new TransactionTemplate(txManager);
        this.retryPolicy = new EtlRetryPolicy(properties);
    }

    /**
     * Run a single processing cycle:
     * - start run -> claim batch -> for each outbox: per-outbox TX { processEvent }, failures recorded after it
     * - finish run and persist counters
     */
    public void runOnce(String pipelineName, int batchSize) {
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

            for (OutboxDto outbox : claimed) {
                if (process(runId, outbox, ingestId)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            // finalize run
            etlRunService.finishRun(EtlRunCompletion.builder().etlRunId(runId).status("finished").submittedCount(submittedCount)
                .successCount(successCount).failureCount(failureCount).minSubmissionSerial(minSerial).maxSubmissionSerial(maxSerial)
                .build());
            log.info("Finished run {}: submitted={}, success={}, failure={}", runId, submittedCount, successCount, failureCount);
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

    /** Projects one claimed row; true when it succeeded. */
    private boolean process(UUID runId, OutboxDto outbox, UUID ingestId) {
        int attempt = outbox.getAttempt();
        if (retryPolicy.wasClaimedTooOften(attempt)) {
            recordDead(runId, outbox, "claimed " + attempt + " times without an outcome");
            return false;
        }
        try {
            txTemplate.executeWithoutResult(status -> eventProcessorService.processEvent(runId, outbox, ingestId));
            return true;
        } catch (RuntimeException e) {
            // The row's transaction has rolled back: nothing of it is left to wait on.
            String error = EtlFailures.describe(e);
            log.debug("Outbox row {} failed on attempt {}", outbox.getOutboxId(), attempt, e);
            if (retryPolicy.isLastAttempt(attempt)) {
                recordDead(runId, outbox, error);
            } else {
                recordRetry(runId, outbox, error, retryPolicy.delayAfter(attempt));
            }
            return false;
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
