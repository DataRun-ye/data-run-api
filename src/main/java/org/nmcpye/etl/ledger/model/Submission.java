package org.nmcpye.etl.ledger.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

// ---------- POJOs (minimal) ----------
@Data
@Builder
public final class Submission {
    private final String id;
    private final String clientTxId;
    private final String txType;
    private final Instant txDate;
    private final String assignmentId;
    private final String campaignId;
    private final String createdBy;
    private final String fromPartyId;
    private final String toPartyId;
    private final String affectedPartyId;
    private final List<SubmissionLine> lines;
}
