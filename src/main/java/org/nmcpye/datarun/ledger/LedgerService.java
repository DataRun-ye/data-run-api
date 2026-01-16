package org.nmcpye.datarun.ledger;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.ledger.model.InventoryLedgerEntry;
import org.nmcpye.datarun.ledger.model.Submission;
import org.nmcpye.datarun.ledger.model.SubmissionLine;
import org.nmcpye.datarun.ledger.repo.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Minimal ledger generator + persister for submission -> inventory_ledger rows.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    /**
     * Transactional: inserts ledger rows, upserts balances, inserts movement_fact and marks submission processed.
     */
    @Transactional
    public void processSubmission(Submission submission) {
        List<InventoryLedgerEntry> entries = buildLedgerEntries(submission);
        ledgerRepository.persistEntries(entries, submission.getSubmissionUid());
    }

    /**
     * double-entry generation
     */
    public List<InventoryLedgerEntry> buildLedgerEntries(Submission s) {
        List<InventoryLedgerEntry> out = new ArrayList<>();
        Instant now = Instant.now();

        for (SubmissionLine line : s.getLines()) {
            BigDecimal qty = line.getQty() == null ? BigDecimal.ZERO : line.getQty();
            String baseLineKey = line.getLineKey();

            switch ((s.getTxType() == null ? "" : s.getTxType())) {
                case "TRANSFER":
                case "PICKUP":
                case "ISSUE":
                    // OUT row (source loses)
                    out.add(buildEntry(s, line, qty.negate(), s.getFromPartyId(), baseLineKey + "::OUT", now));
                    // IN row (destination gains)
                    out.add(buildEntry(s, line, qty, s.getToPartyId(), baseLineKey + "::IN", now));
                    break;

                case "RECEIPT":
                    out.add(buildEntry(s, line, qty, s.getToPartyId(), baseLineKey + "::IN", now));
                    break;

                case "RETURN":
                    // team returns to wh: team -> wh
                    out.add(buildEntry(s, line, qty.negate(), s.getFromPartyId(), baseLineKey + "::OUT", now));
                    out.add(buildEntry(s, line, qty, s.getToPartyId(), baseLineKey + "::IN", now));
                    break;

                case "ADJUSTMENT":
                    // use line.partyId or fallback to fromPartyId
                    String affected = line.getPartyId() != null ? line.getPartyId() : s.getFromPartyId();
                    out.add(buildEntry(s, line, qty, affected, baseLineKey + "::ADJ", now));
                    break;

                case "STOCK_COUNT":
                    // policy: write adjustments externally; skip here or convert stockcount -> adjustments
                    break;

                case "CONSUMPTION":
                    // we usually do not alter ledger for consumption; skip
                    break;

                default:
                    // conservative: create adjustment to org unit if available
                    String p = s.getToPartyId() != null ? s.getToPartyId() : s.getFromPartyId();
                    out.add(buildEntry(s, line, qty, p, baseLineKey + "::ADHOC", now));
            }
        }
        return out;
    }

    private InventoryLedgerEntry buildEntry(Submission s, SubmissionLine l, BigDecimal qtyDelta, String partyId, String submissionLineId, Instant now) {
        return InventoryLedgerEntry.builder()
            .id(CodeGenerator.nextUlid())
            .submissionId(s.getSubmissionId())
            .submissionLineId(submissionLineId)
            .transactionType(s.getTxType())
            .txDate(s.getTxDate())
            .assignmentId(s.getAssignmentUid())
            .fromPartyId(s.getFromPartyId())
            .toPartyId(s.getToPartyId())
            .partyId(partyId)
            .skuId(l.getSkuId())
            .batchId(l.getBatchId())
            .qtyDelta(qtyDelta)
            .unit(l.getUnit() == null ? "unit" : l.getUnit())
            .provenanceJson(makeProvenance(s, l))
            .createdAt(now)
            .build();
    }

    private String makeProvenance(Submission s, SubmissionLine l) {
        // simple JSON string; you may want to use ObjectMapper for richer structure
        return String.format("{\"submissionUid\":\"%s\",\"templateUid\":\"%s\",\"assignmentUid\":\"%s\",\"lineKey\":\"%s\"}",
            s.getSubmissionUid(), s.getTemplateUid(), s.getAssignmentUid(), l.getLineKey());
    }
}
