package org.nmcpye.datarun.etl.service;

import org.nmcpye.datarun.etl.dto.OutboxDto;
import org.nmcpye.datarun.etl.model.TallCanonicalRow;

import java.util.List;

/**
 * Transformation service placeholder.
 * This interface will later be implemented to:
 * - convert a submission/outbox payload into tall-table rows
 * - perform enrichment/validation
 * - write to tall_table with idempotent upserts
 * <p>
 * NOTE: For this phase we leave methods empty. Implementations must be idempotent.
 */
public interface TransformService {

    /**
     * Transform the outbox payload and write to target tall tables.
     * <p>
     * - ingestId must be used as idempotency token for all writes.
     * - This method will eventually return a transform result object (e.g., rowsWritten).
     */
    List<TallCanonicalRow> transform(OutboxDto outbox) throws Exception;
}
