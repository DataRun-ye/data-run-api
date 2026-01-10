package org.nmcpye.etl.ledger.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public final class SubmissionLine {
    private final String id;
    private final String skuId;
    private final String batchId;
    private final BigDecimal quantity;
    private final String unit;
    private final String partyId;
}
