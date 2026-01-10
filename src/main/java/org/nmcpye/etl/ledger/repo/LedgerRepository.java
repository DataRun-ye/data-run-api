package org.nmcpye.etl.ledger.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nmcpye.etl.ledger.model.InventoryLedgerEntry;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Minimal ledger generator + persister for submission -> inventory_ledger rows.
 */
@Repository
@RequiredArgsConstructor
public class LedgerRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    private static final String sql = """
        INSERT INTO inventory_ledger (
           id, submission_id, submission_line_id, transaction_type, tx_date,
           assignment_id, from_party_id, to_party_id, party_id,
           sku_id, batch_id, qty_delta, unit, provenance, created_at
        ) VALUES (
           :id, :submissionId, :submissionLineId, :transactionType, :txDate,
           :assignmentId, :fromPartyId, :toPartyId, :partyId,
           :skuId, :batchId, :qtyDelta, :unit, CAST(:provenance AS jsonb), :createdAt
        )
        ON CONFLICT (submission_id, submission_line_id) DO NOTHING
        """;

    /**
     * Persist entries idempotently. Uses ON CONFLICT (submission_id, submission_line_id) DO NOTHING.
     * Adjust the INSERT columns to match your inventory_ledger schema.
     */
    public void persistEntries(List<InventoryLedgerEntry> entries) {
        MapSqlParameterSource[] batch = entries.stream().map(e -> {
            MapSqlParameterSource m = new MapSqlParameterSource();
            m.addValue("id", e.getId());
            m.addValue("submissionId", e.getSubmissionId());
            m.addValue("submissionLineId", e.getSubmissionLineId());
            m.addValue("transactionType", e.getTransactionType());
            m.addValue("txDate", e.getTxDate());
            m.addValue("assignmentId", e.getAssignmentId());
            m.addValue("fromPartyId", e.getFromPartyId());
            m.addValue("toPartyId", e.getToPartyId());
            m.addValue("partyId", e.getPartyId());
            m.addValue("skuId", e.getSkuId());
            m.addValue("batchId", e.getBatchId());
            m.addValue("qtyDelta", e.getQtyDelta());
            m.addValue("unit", e.getUnit());
            try {
                m.addValue("provenance", mapper.writeValueAsString(e.getProvenance()));
            } catch (Exception ex) {
                m.addValue("provenance", "{}");
            }
            m.addValue("createdAt", e.getCreatedAt());
            return m;
        }).toArray(MapSqlParameterSource[]::new);

        jdbc.batchUpdate(sql, batch);
    }
}
