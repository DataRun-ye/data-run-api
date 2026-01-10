package org.nmcpye.etl.ledger.model;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class InventoryLedgerEntry {
    // getters (omitted for brevity — add if needed)
    private String id;
    private String submissionId;
    private String submissionLineId;
    private String transactionType;
    private Instant txDate;
    private String assignmentId;
    private String fromPartyId;
    private String toPartyId;
    private String partyId;
    private String skuId;
    private String batchId;
    private BigDecimal qtyDelta;
    private String unit;
    private Map<String, Object> provenance;
    private Instant createdAt;
}
