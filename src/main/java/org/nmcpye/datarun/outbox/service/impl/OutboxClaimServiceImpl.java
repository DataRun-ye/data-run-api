package org.nmcpye.datarun.outbox.service.impl;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.repository.OutboxClaimPort;
import org.nmcpye.datarun.outbox.service.OutboxClaimService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Service that claims pending outbox rows for processing.
 * <p>
 * Notes:
 * - This delegates to OutboxJdbcRepository.claimPendingBatch which uses FOR UPDATE SKIP LOCKED
 * to safely claim rows in concurrent consumer scenarios.
 * - Keep this layer thin; it may later add metrics, throttling, or claim-scoping logic.
 */
@Service
@RequiredArgsConstructor
public class OutboxClaimServiceImpl implements OutboxClaimService {

    private static final Logger log = LoggerFactory.getLogger(OutboxClaimServiceImpl.class);

    private final OutboxClaimPort outboxJdbcRepository;
    private final EtlProperties etlProperties;

    /**
     * Claim up to batchSize pending outbox rows and assign them to ingestId.
     * <p>
     * Concurrency:
     * - Under the hood we use "FOR UPDATE SKIP LOCKED" in the JDBC repository. This ensures multiple
     * concurrent claimers do not pick the same rows; SKIP LOCKED causes locked rows to be skipped.
     *
     * @param ingestId  UUID to assign to claimed rows
     * @param batchSize max rows to claim
     * @return list of claimed OutboxDto (may be empty)
     */
    @Override
    public List<OutboxDto> claimBatchWithIngestId(UUID ingestId, String claimedBy, int batchSize) {
        // We intentionally do not persist the ingestId into EtlRun here; caller/ orchestrator manages run metadata.
        log.debug("Attempting to claim up to {} outbox rows with ingestId={}", batchSize, ingestId);
        List<OutboxDto> claimed =
            outboxJdbcRepository.claimPendingBatch(ingestId, claimedBy, batchSize, etlProperties.getClaimLease());
        int n = (claimed == null) ? 0 : claimed.size();
        log.debug("Claimed {} outbox rows with ingestId={}", n, ingestId);
        return (claimed == null) ? List.of() : claimed;
    }

    @Override
    public OptionalInt startAttempt(long outboxId, UUID ingestId) {
        return outboxJdbcRepository.startAttempt(outboxId, ingestId);
    }

    @Override
    public int release(UUID ingestId, Collection<Long> outboxIds) {
        return outboxJdbcRepository.release(ingestId, outboxIds);
    }
}
