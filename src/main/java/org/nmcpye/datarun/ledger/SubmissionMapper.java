package org.nmcpye.datarun.ledger;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.ledger.model.Submission;
import org.nmcpye.datarun.ledger.model.SubmissionLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubmissionMapper {

    private final MappingCache mappingCache;

    /**
     * Map pivot rows (each row from pivot.fact_receipts_and_returns_9xx) into one Submission DTO.
     * Expects rows already filtered by submission_uid.
     */
    public Submission map(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return null;

        Map<String, Object> h = rows.get(0);
        Submission.SubmissionBuilder sb = Submission.builder()
            .submissionUid(safeStr(h.get("submission_uid")))
            .submissionId(safeStr(h.get("submission_serial")))
            .templateUid(safeStr(h.get("template_uid")))
            .txType(safeStr(h.get("tx_type")))
            .flowType(safeStr(h.get("flow_type")))
            .assignmentUid(safeStr(h.get("assigned_assignment_uid")))
            .activityUid(safeStr(h.get("activity_uid")))
            .teamUid(safeStr(h.get("assigned_team_uid")))
            .orgUnitUid(safeStr(h.get("assigned_org_unit_uid")))
            .createdByUser(safeStr(h.get("created_by_user")))
            .assignedTeamCode(safeStr(h.get("assigned_team_code")))
            .txDate(parseInstant(h.get("tx_date")))
            .startTime(parseInstant(h.get("start_time")));

        // Group rows into lines by event_id + category_uid (or category if uid null)
        Map<String, SubmissionLine.SubmissionLineBuilder> grouped = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String eventId = safeStr(r.get("event_id"));
            String catUid = safeStr(r.get("category_uid"));
            String category = safeStr(r.get("category"));
//            String key = eventId + "::" + (catUid != null ? catUid : category);

            SubmissionLine.SubmissionLineBuilder lb = grouped.computeIfAbsent(eventId, k ->
                SubmissionLine.builder().lineKey(k));

            // set fields (we assume pivot has one line per repeat; if multiple, last wins for non-qty)
            BigDecimal qty = parseBigDecimal(r.get("qty"));
            lb.qty(qty == null ? BigDecimal.ZERO : qty);
            lb.category(category);
            lb.categoryUid(catUid);
            lb.batchId(safeStr(r.get("expiry_date")));
            lb.expiryDate(safeStr(r.get("expiry_date")));
            lb.unit("unit");
        }

        Submission s = sb.build();
        for (SubmissionLine.SubmissionLineBuilder lb : grouped.values()) {
            SubmissionLine line = lb.build();
            // resolve SKU (from canonical->sku map)
            String sku = mappingCache.resolveSku(line.getCategoryUid(), line.getCategory());
            line.setSkuId(sku != null ? sku : line.getCategory());
            s.getLines().add(line);
        }

        // resolve parties (from/to) using mapping cache and submission header
        mappingCache.resolvePartiesForSubmission(s);

        return s;
    }

    private static String safeStr(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Instant parseInstant(Object o) {
        if (o == null) return null;
        if (o instanceof Instant) return (Instant) o;
        if (o instanceof Timestamp) return ((Timestamp) o).toInstant();
        String s = String.valueOf(o);
        try {
            return Instant.parse(s);
        } catch (Exception ignored1) {
            try {
                // Date-only → start of day UTC
                return LocalDate.parse(s)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(((Number) o).toString());
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (Exception ex) {
            return null;
        }
    }
}

