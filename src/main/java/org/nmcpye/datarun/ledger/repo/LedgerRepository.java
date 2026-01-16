package org.nmcpye.datarun.ledger.repo;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.ledger.model.InventoryLedgerEntry;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class LedgerRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String INSERT_INVENTORY_LEDGER =
        "INSERT INTO analytics.inventory_ledger (id, submission_id, submission_line_id, transaction_type, tx_date, " +
            "assignment_id, from_party_id, to_party_id, party_id, sku_id, batch_id, qty_delta, unit, provenance, created_at) " +
            "VALUES (:id, :submissionId, :submissionLineId, :transactionType, :txDate, " +
            ":assignmentId, :fromPartyId, :toPartyId, :partyId, :skuId, :batchId, :qtyDelta, :unit, :provenance::jsonb, :createdAt) " +
            "ON CONFLICT (submission_id, submission_line_id) DO NOTHING";

    private static final String UPSERT_STOCK_BALANCE =
        "INSERT INTO analytics.stock_balance (party_id, sku_id, batch_id, qty_on_hand, last_updated) " +
            "VALUES (:partyId, :skuId, coalesce(:batchId,'-'), :qtyOnHand, now()) " +
            "ON CONFLICT (party_id, sku_id, batch_id) DO UPDATE " +
            "SET qty_on_hand = analytics.stock_balance.qty_on_hand + EXCLUDED.qty_on_hand, last_updated = now()";

    private static final String INSERT_MOVEMENT_FACT =
        "INSERT INTO analytics.movement_fact (id, submission_id, tx_date, tx_type, assignment_id, from_party_id, to_party_id, party_id, sku_id, batch_id, qty, direction, user_id, team_id, campaign_id, created_at) " +
            "VALUES (:id, :submissionId, :txDate, :txType, :assignmentId, :fromPartyId, :toPartyId, :partyId, :skuId, :batchId, :qty, :direction, :userId, :teamId, :campaignId, :createdAt) " +
            "ON CONFLICT (id) DO NOTHING";

    private static final String MARK_PROCESSED =
        "UPDATE analytics.events SET processed_to_ledger = true, ledger_claimed_by = NULL, ledger_claimed_at = NULL WHERE submission_uid = :submissionUid";

    public void persistEntries(List<InventoryLedgerEntry> entries, String submissionUid) {
        if (entries == null || entries.isEmpty()) {
            // still mark processed to avoid reprocessing empty submissions
            jdbc.update(MARK_PROCESSED, new MapSqlParameterSource("submissionUid", submissionUid));
            return;
        }

        // 1) insert ledger rows idempotently (batch)
        MapSqlParameterSource[] ledgerParams = entries.stream().map(this::toLedgerParams).toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate(INSERT_INVENTORY_LEDGER, ledgerParams);

        // 2) aggregate deltas for stock upserts (group by party,sku,batchKey)
        Map<StockKey, BigDecimal> agg = new HashMap<>();
        for (InventoryLedgerEntry e : entries) {
            String batchKey = e.getBatchId() == null ? "-" : e.getBatchId();
            StockKey k = new StockKey(e.getPartyId(), e.getSkuId(), batchKey);
            agg.merge(k, e.getQtyDelta(), BigDecimal::add);
        }

        // 3) upsert aggregated balances
        List<MapSqlParameterSource> upserts = agg.entrySet().stream()
            .map(kv -> new MapSqlParameterSource()
                .addValue("partyId", kv.getKey().partyId)
                .addValue("skuId", kv.getKey().skuId)
                .addValue("batchId", kv.getKey().batchId.equals("-") ? null : kv.getKey().batchId)
                .addValue("qtyOnHand", kv.getValue()))
            .toList();

        for (MapSqlParameterSource up : upserts) {
            jdbc.update(UPSERT_STOCK_BALANCE, up);
        }

        // 4) insert movement_fact rows (one per ledger entry)
        MapSqlParameterSource[] movementParams = entries.stream().map(this::toMovementParams).toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate(INSERT_MOVEMENT_FACT, movementParams);

        // 5) mark processed
        jdbc.update(MARK_PROCESSED, new MapSqlParameterSource("submissionUid", submissionUid));
    }

    private MapSqlParameterSource toLedgerParams(InventoryLedgerEntry e) {
        return new MapSqlParameterSource()
            .addValue("id", e.getId())
            .addValue("submissionId", e.getSubmissionId())
            .addValue("submissionLineId", e.getSubmissionLineId())
            .addValue("transactionType", e.getTransactionType())
            .addValue("txDate", e.getTxDate() != null ? Timestamp.from(e.getTxDate()) : null)
            .addValue("assignmentId", e.getAssignmentId())
            .addValue("fromPartyId", e.getFromPartyId())
            .addValue("toPartyId", e.getToPartyId())
            .addValue("partyId", e.getPartyId())
            .addValue("skuId", e.getSkuId())
            .addValue("batchId", e.getBatchId())
            .addValue("qtyDelta", e.getQtyDelta())
            .addValue("unit", e.getUnit())
            .addValue("provenance", e.getProvenanceJson())
            .addValue("createdAt", e.getCreatedAt() != null ? Timestamp.from(e.getCreatedAt()) : null);
    }

    private MapSqlParameterSource toMovementParams(InventoryLedgerEntry e) {
        String direction = e.getQtyDelta().signum() >= 0 ? "IN" : "OUT";
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("id", e.getId())
            .addValue("submissionId", e.getSubmissionId())
            .addValue("txDate", e.getTxDate() != null ? Timestamp.from(e.getTxDate()) : null)
            .addValue("txType", e.getTransactionType())
            .addValue("assignmentId", e.getAssignmentId())
            .addValue("fromPartyId", e.getFromPartyId())
            .addValue("toPartyId", e.getToPartyId())
            .addValue("partyId", e.getPartyId())
            .addValue("skuId", e.getSkuId())
            .addValue("batchId", e.getBatchId())
            .addValue("qty", e.getQtyDelta().abs())
//            .addValue("unit", e.getUnit())
            .addValue("direction", direction)
            .addValue("userId", null)
            .addValue("teamId", e.getAssignmentId())
            .addValue("campaignId", null)
            .addValue("createdAt", e.getCreatedAt() != null ? Timestamp.from(e.getCreatedAt()) : null);
        return p;
    }

    private static class StockKey {
        final String partyId;
        final String skuId;
        final String batchId;

        StockKey(String p, String s, String b) {
            this.partyId = p;
            this.skuId = s;
            this.batchId = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StockKey)) return false;
            StockKey k = (StockKey) o;
            return Objects.equals(partyId, k.partyId) && Objects.equals(skuId, k.skuId) && Objects.equals(batchId, k.batchId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(partyId, skuId, batchId);
        }
    }
}
