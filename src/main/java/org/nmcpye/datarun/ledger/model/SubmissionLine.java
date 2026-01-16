package org.nmcpye.datarun.ledger.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public final class SubmissionLine {
    // deterministic key within submission; we will append IN/OUT suffix when creating ledger rows
    private String lineKey;
    private String category;
    private String categoryUid;
    private BigDecimal qty;
    private String unit;
    private String batchId;
    private String expiryDate;

    // resolved later
    private String skuId;

    // optional: if line carries its own party override
    private String partyId;
}
