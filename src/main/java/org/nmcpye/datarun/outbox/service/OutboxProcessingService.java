package org.nmcpye.datarun.outbox.service;

import org.nmcpye.datarun.outbox.dto.OutboxDto;

import java.time.Duration;
import java.util.UUID;

/**
 * Records per-outbox processing outcomes into outbox_processing and updates outbox rows.
 * <p>
 * Success is recorded inside the transaction that projected the row, so both commit together. Failures are
 * recorded in a transaction of their own, after the projecting transaction has rolled back.
 */
public interface OutboxProcessingService {

    /**
     * Record success for the outbox row.
     * Implementation should:
     * - insert outbox_processing status 'success'
     * - update outbox.status = 'success'
     * - ensure idempotency (do not double-insert)
     */
    void recordSuccess(UUID etlRunId, OutboxDto outbox);

    /**
     * Record a failure that will be tried again: an outbox_processing row with status 'failure', and the outbox
     * row marked 'failed' with its error, to be claimed again after {@code retryAfter}.
     */
    void recordRetry(UUID etlRunId, OutboxDto outbox, String error, Duration retryAfter);

    /**
     * Record a failure that will not be tried again: an outbox_processing row with status 'dead', and the outbox
     * row marked 'dead' with its error.
     */
    void recordDead(UUID etlRunId, OutboxDto outbox, String error);
}
