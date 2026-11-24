package org.nmcpye.datarun.outbox.repository;

import org.nmcpye.datarun.outbox.dto.OutboxDto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxClaimPort {
    /**
     * Atomically claim up to {@code limit} due outbox rows, lease them for {@code lease}, count the attempt,
     * and return them in claim order (submission serial, then outbox id).
     */
    List<OutboxDto> claimPendingBatch(UUID ingestId, String claimedBy, int limit, Duration lease);

    /**
     * Mark outbox row success only if ingest_id matches; returns true if updated.
     */
    boolean markSuccessIfIngestMatches(long outboxId, UUID ingestId);

    /**
     * Mark outbox row success (no ingest check).
     */
    void markSuccess(long outboxId);

    /**
     * Mark outbox row failed and return the new attempt count.
     */
    int markFailure(long outboxId, String error, Instant nextAttemptAt);
}
