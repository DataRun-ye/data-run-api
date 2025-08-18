package org.nmcpye.datarun.jpa.outbox;

import java.time.Instant;
import java.util.List;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
public interface OutboxEventClaimsRepository {

    /**
     * Atomically claim up to `limit` PENDING rows and set them to PROCESSING.
     * Increments attempts counter immediately.
     */
    List<OutboxEvent> claimBatch(int limit);

    /** Mark event DONE and set processedAt. */
    void markDone(long id, Instant processedAt);

    /**
     * Mark event back to PENDING with backoff. Also sets lastError and increments attempts if not already done.
     * You may pass the next availability timestamp (computed by your backoff policy).
     */
    void reschedule(long id, String errorMessage, Instant nextAvailableAt);

    /** Move to FAILED permanently (no further retries). */
    void markFailed(long id, String errorMessage);
}
