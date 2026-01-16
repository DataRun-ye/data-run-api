package org.nmcpye.datarun.ledger.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// ---------- POJOs (minimal) ----------
@Data
@Builder
public final class Submission {
    private String submissionUid;
    private String submissionId;      // numeric serial or id
    private String templateUid;
    private String txType;           // RECEIPT|ISSUE|TRANSFER|RETURN|STOCK_COUNT|ADJUSTMENT|CONSUMPTION
    private String flowType;         // HF_RECEIPT | WH_TEAM_RECEIPT | ...
    private String assignmentUid;
    private String activityUid;
    private String teamUid;
    private String orgUnitUid;
    private Instant txDate;
    private Instant startTime;
    private String createdByUser;
    private String assignedTeamCode;
    @Builder.Default
    private List<SubmissionLine> lines = new ArrayList<>();

    // resolved parties (populated by mapper)
    private String fromPartyId;
    private String toPartyId;
}
