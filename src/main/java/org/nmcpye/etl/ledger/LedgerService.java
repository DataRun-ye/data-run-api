package org.nmcpye.etl.ledger;

import lombok.RequiredArgsConstructor;
import org.nmcpye.etl.ledger.model.InventoryLedgerEntry;
import org.nmcpye.etl.ledger.model.Submission;
import org.nmcpye.etl.ledger.model.SubmissionLine;
import org.nmcpye.etl.ledger.repo.LedgerRepository;
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
     * Build ledger entries for the submission and persist them idempotently.
     * Call inside a transactional boundary if you want atomicity across rows.
     */
    @Transactional
    public void processSubmission(Submission submission) {
        List<InventoryLedgerEntry> entries = buildLedgerEntries(submission);
        if (entries.isEmpty()) return;
        ledgerRepository.persistEntries(entries);
    }

    /**
     * Build the list of ledger entries for a submission.
     */
    public List<InventoryLedgerEntry> buildLedgerEntries(Submission s) {
        List<InventoryLedgerEntry> out = new ArrayList<>();
        Instant now = Instant.now();

        for (SubmissionLine line : s.getLines()) {
            BigDecimal qty = line.getQuantity();
            String unit = line.getUnit();
            String submissionId = s.getId();
            String lineId = line.getId();

            switch (s.getTxType()) {
                case "TRANSFER": // produce OUT (from) and IN (to)
                case "PICKUP":   // pickup is transfer MU -> TEAM
                    // OUT
                    out.add(InventoryLedgerEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .submissionId(submissionId)
                        .submissionLineId(lineId)
                        .transactionType(s.getTxType())
                        .txDate(s.getTxDate())
                        .assignmentId(s.getAssignmentId())
                        .fromPartyId(s.getFromPartyId())
                        .toPartyId(s.getToPartyId())
                        .partyId(s.getFromPartyId())   // affected = from (out)
                        .skuId(line.getSkuId())
                        .batchId(line.getBatchId())
                        .qtyDelta(qty.negate())
                        .unit(unit)
                        .provenance(provenanceFor(s))
                        .createdAt(now)
                        .build());
                    // IN
                    out.add(InventoryLedgerEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .submissionId(submissionId)
                        .submissionLineId(lineId)
                        .transactionType(s.getTxType())
                        .txDate(s.getTxDate())
                        .assignmentId(s.getAssignmentId())
                        .fromPartyId(s.getFromPartyId())
                        .toPartyId(s.getToPartyId())
                        .partyId(s.getToPartyId())     // affected = to (in)
                        .skuId(line.getSkuId())
                        .batchId(line.getBatchId())
                        .qtyDelta(qty)                 // positive in
                        .unit(unit)
                        .provenance(provenanceFor(s))
                        .createdAt(now)
                        .build());
                    break;

                case "ISSUE":
                    // OUT only (from loses)
                    out.add(InventoryLedgerEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .submissionId(submissionId)
                        .submissionLineId(lineId)
                        .transactionType("ISSUE")
                        .txDate(s.getTxDate())
                        .assignmentId(s.getAssignmentId())
                        .fromPartyId(s.getFromPartyId())
                        .toPartyId(s.getToPartyId())
                        .partyId(s.getFromPartyId())
                        .skuId(line.getSkuId())
                        .batchId(line.getBatchId())
                        .qtyDelta(qty.negate())
                        .unit(unit)
                        .provenance(provenanceFor(s))
                        .createdAt(now)
                        .build());
                    // AND a corresponding IN for recipient to keep HF balance consistent
                    out.add(InventoryLedgerEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .submissionId(submissionId)
                        .submissionLineId(lineId)
                        .transactionType("ISSUE") // still ISSUE provenance
                        .txDate(s.getTxDate())
                        .assignmentId(s.getAssignmentId())
                        .fromPartyId(s.getFromPartyId())
                        .toPartyId(s.getToPartyId())
                        .partyId(s.getToPartyId())
                        .skuId(line.getSkuId())
                        .batchId(line.getBatchId())
                        .qtyDelta(qty)
                        .unit(unit)
                        .provenance(provenanceFor(s))
                        .createdAt(now)
                        .build());
                    break;

                case "RECEIPT":
                    // IN only (to party receives)
                    out.add(InventoryLedgerEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .submissionId(submissionId)
                        .submissionLineId(lineId)
                        .transactionType("RECEIPT")
                        .txDate(s.getTxDate())
                        .assignmentId(s.getAssignmentId())
                        .fromPartyId(s.getFromPartyId())
                        .toPartyId(s.getToPartyId())
                        .partyId(s.getToPartyId())
                        .skuId(line.getSkuId())
                        .batchId(line.getBatchId())
                        .qtyDelta(qty)
                        .unit(unit)
                        .provenance(provenanceFor(s))
                        .createdAt(now)
                        .build());
                    break;

                case "ADJUSTMENT":
                    // Adjustment applies to a single party specified in submission (or line)
                    String affected = Optional.ofNullable(line.getPartyId()).orElse(s.getAffectedPartyId());
                    out.add(InventoryLedgerEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .submissionId(submissionId)
                        .submissionLineId(lineId)
                        .transactionType("ADJUSTMENT")
                        .txDate(s.getTxDate())
                        .assignmentId(s.getAssignmentId())
                        .fromPartyId(s.getFromPartyId())
                        .toPartyId(s.getToPartyId())
                        .partyId(affected)
                        .skuId(line.getSkuId())
                        .batchId(line.getBatchId())
                        .qtyDelta(line.getQuantity()) // positive or negative based on submission sign
                        .unit(unit)
                        .provenance(provenanceFor(s))
                        .createdAt(now)
                        .build());
                    break;

                case "CONSUMPTION":
                    // By default we DON'T mutate ledger on consumption. If you want to decrement, handle here.
                    // skip or create special consumption_fact elsewhere.
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported tx type: " + s.getTxType());
            }
        }

        return out;
    }

    private Map<String, Object> provenanceFor(Submission s) {
        Map<String, Object> p = new HashMap<>();
        p.put("submissionId", s.getId());
        p.put("clientTxId", s.getClientTxId());
        p.put("assignmentId", s.getAssignmentId());
        p.put("campaignId", s.getCampaignId());
        p.put("userId", s.getCreatedBy());
        p.put("from", s.getFromPartyId());
        p.put("to", s.getToPartyId());
        return p;
    }
}
