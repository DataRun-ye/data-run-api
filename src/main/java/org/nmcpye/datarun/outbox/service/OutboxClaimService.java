package org.nmcpye.datarun.outbox.service;

import org.nmcpye.datarun.outbox.dto.OutboxDto;

import java.util.List;
import java.util.UUID;

/**
 * Claiming service for outbox rows. This uses NamedParameterJdbcTemplate based repository
 * to atomically claim rows (assign ingest_id) for a run.
 * <p>
 * Responsibilities:
 * - claim batches using SKIP LOCKED / UPDATE ... RETURNING
 * - return list of OutboxDto for processing
 */
public interface OutboxClaimService {

    /**
     * Claim up to {@code batchSize} due rows under the given ingestId, leased to the claiming run.
     */
    List<OutboxDto> claimBatchWithIngestId(UUID ingestId, String claimedBy, int batchSize);
}
