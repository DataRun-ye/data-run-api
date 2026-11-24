package org.nmcpye.datarun.outbox.repository;

import org.nmcpye.datarun.outbox.dto.OutboxDto;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

public interface OutboxClaimPort {
    /**
     * Atomically claim up to {@code limit} due outbox rows, lease them for {@code lease}, and return them in claim
     * order (submission serial, then outbox id). A claim does not count an attempt.
     */
    List<OutboxDto> claimPendingBatch(UUID ingestId, String claimedBy, int limit, Duration lease);

    /**
     * Count an attempt for a claimed row whose processing starts, in a transaction of its own so the count
     * survives the process dying on that row; the new count, or empty when the row now belongs to a newer claim.
     */
    OptionalInt startAttempt(long outboxId, UUID ingestId);

    /** End the lease of claimed rows that were not finished; returns how many were released. */
    int release(UUID ingestId, Collection<Long> outboxIds);

    /**
     * Mark outbox row success (no ingest check).
     */
    void markSuccess(long outboxId);

    /**
     * Mark the row failed, to be claimed again after {@code retryAfter}; false when the row now belongs to a
     * newer claim.
     */
    boolean markRetry(long outboxId, UUID ingestId, String error, Duration retryAfter);

    /**
     * Mark the row dead: it is not claimed again until an operator puts it back to pending; false when the row
     * now belongs to a newer claim.
     */
    boolean markDead(long outboxId, UUID ingestId, String error);
}
