package org.nmcpye.datarun.jpa.datasubmissionoutbox.repository;

import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;

import java.time.Instant;
import java.util.List;

/**
 * @author Hamza Assada
 * @since 15/08/2025
 */
public interface OutboxEventClaimsRepository {

    /**
     * Atomically claim up to `limit` PENDING rows and set them to PROCESSING.
     * Increments attempts counter immediately.
     */
    List<OutboxEvent> claimBatch(int limit);

    List<OutboxEvent> claimBatch(int limit, String eventType, String owner);

    /**
     * Mark event DONE and set processedAt.
     */
    void markDone(long id, Instant processedAt);

    /**
     * Mark event back to PENDING with backoff. Also sets lastError and increments attempts if not already done.
     * You may pass the next availability timestamp (computed by your backoff policy).
     */
    void reschedule(long id, String errorMessage, Instant nextAvailableAt);

//    void markFailed(long id, String errorMessage);

    /**
     * Move to FAILED permanently (no further retries).
     */
    void markDlq(long id, String lastMessage);

    List<Long> reclaimStale(int visibilitySeconds);
}
