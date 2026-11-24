package org.nmcpye.datarun.outbox.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.etl.service.EtlRunService;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.dto.OutboxProcessingDto;
import org.nmcpye.datarun.outbox.repository.OutboxClaimPort;
import org.nmcpye.datarun.outbox.repository.OutboxProcessingJdbcRepository;
import org.nmcpye.datarun.outbox.service.OutboxProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service responsible for recording processing outcomes and updating outbox status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessingServiceImpl implements OutboxProcessingService {
    private static final int ERROR_MAX_LEN = 1000;

    private final OutboxProcessingJdbcRepository processingJdbcRepository;
    private final OutboxClaimPort outboxJdbcRepository;
    private final EtlRunService etlRunService;

    /**
     * Record success for the outbox row.
     * <p>
     * Transactional & idempotency behavior:
     * - This method is transactional: inserted outbox_processing and outbox table update happen in one TX.
     * - If a success row for (etlRunId, outboxId) already exists, we do nothing (idempotent).
     * - We insert the success bookkeeping row and THEN update the outbox table to 'success'.
     * <p>
     * Reasoning: inserting the processing row first gives us a durable audit entry; joining the repository
     * transaction means if markOutboxSuccess fails the TX will rollback and no partial state remains.
     */
    @Override
    @Transactional
    public void recordSuccess(UUID etlRunId, OutboxDto outbox) {
        audit(etlRunId, outbox, "success", null);

        // update outbox status to 'success' (joins same transaction)
        outboxJdbcRepository.markSuccess(outbox.getOutboxId());

        // increment run-level success counter
        etlRunService.incrementSuccess(etlRunId, 1);
    }

    @Override
    @Transactional
    public void recordRetry(UUID etlRunId, OutboxDto outbox, String error, Duration retryAfter) {
        String safeError = truncate(error);
        audit(etlRunId, outbox, "failure", safeError);
        if (!outboxJdbcRepository.markRetry(outbox.getOutboxId(), outbox.getIngestId(), safeError, retryAfter)) {
            log.warn("Outbox row {} was claimed again by a newer run; its failure is audited only", outbox.getOutboxId());
        }
        etlRunService.incrementFailure(etlRunId, 1);
    }

    @Override
    @Transactional
    public void recordDead(UUID etlRunId, OutboxDto outbox, String error) {
        String safeError = truncate(error);
        audit(etlRunId, outbox, "dead", safeError);
        if (!outboxJdbcRepository.markDead(outbox.getOutboxId(), outbox.getIngestId(), safeError)) {
            log.warn("Outbox row {} was claimed again by a newer run; its failure is audited only", outbox.getOutboxId());
        }
        etlRunService.incrementFailure(etlRunId, 1);
    }

    private void audit(UUID etlRunId, OutboxDto outbox, String status, String error) {
        processingJdbcRepository.upsertOutboxProcessing(OutboxProcessingDto.builder()
            .etlRunId(etlRunId)
            .outboxId(outbox.getOutboxId())
            .submissionSerialNumber(outbox.getSubmissionSerialNumber())
            .submissionUid(outbox.getSubmissionUid())
            .submissionId(outbox.getSubmissionId())
            .status(status)
            .attempt(outbox.getAttempt())
            .error(error)
            .processedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS))
            .build());
    }

    private static String truncate(String error) {
        String safeError = (error == null) ? "unspecified error" : error;
        return safeError.length() > ERROR_MAX_LEN ? safeError.substring(0, ERROR_MAX_LEN) + "...(truncated)" : safeError;
    }
}
