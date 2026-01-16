package org.nmcpye.datarun.ledger.model;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class InventoryLedgerEntry {
    private String id;
    private String submissionId;
    private String submissionLineId;
    private String transactionType;
    private Instant txDate;
    private String assignmentId;
    private String fromPartyId;
    private String toPartyId;
    private String partyId;  // whose balance this row affects
    private String skuId;
    private String batchId;
    private BigDecimal qtyDelta;
    private String unit;
    private String provenanceJson;
    private Instant createdAt;
}
