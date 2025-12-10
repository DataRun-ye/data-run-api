package org.nmcpye.datarun.analytics.domaintabletoolkit.pivot;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationService {
    private final JdbcTemplate jdbc;

    public ValidationResult validateSubmissionTable(String activityId) {
        String factsTable = "analytics.facts_wide_" + sanitize(activityId) + "_new";
        String sql = String.format("""
            SELECT (SELECT COUNT(*) FROM %s) AS facts_rows,
            (SELECT COUNT(DISTINCT submission_uid) FROM analytics.submissions_enriched
             WHERE activity_id = '%s') AS submissions
            """, factsTable, activityId);
        return jdbc.queryForObject(sql, (rs, rn) -> {
            long facts = rs.getLong("facts_rows");
            long subs = rs.getLong("submissions");
            double ratio = subs == 0 ? 1.0 : ((double) facts) / subs;
            ValidationResult vr = new ValidationResult();
            vr.passed(ratio >= 0.9);
            vr.factsRows(facts);
            vr.submissionCount(subs);
            vr.ratio(ratio);
            return vr;
        });
    }

    public ValidationResult validateRepeatsTable(String activityId) {
        // Basic validation: ensure table exists and has rows or is empty intentionally
        String factsTable = "analytics.facts_wide_" + sanitize(activityId) + "_repeats_new";
        String sql = String.format("SELECT COUNT(*) AS cnt FROM %s", factsTable);
        try {
            long cnt = jdbc.queryForObject(sql, Long.class);
            ValidationResult vr = new ValidationResult();
            vr.passed(true);
            vr.factsRows(cnt);
            vr.submissionCount(0);
            vr.ratio(1.0);
            return vr;
        } catch (Exception ex) {
            ValidationResult vr = new ValidationResult();
            vr.passed(false);
            vr.factsRows(0);
            vr.submissionCount(0);
            vr.ratio(0);
            vr.message(ex.getMessage());
            return vr;
        }
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public ValidationResult validateTemplateTable(String baseFq, String templateUid) {
        String factsTable = Naming.newName(baseFq);   // analytics.fact_<sanitized>_new

        String sql = String.format(
            "SELECT (SELECT COUNT(*) FROM %s) AS facts_rows, " +
                "(SELECT COUNT(DISTINCT submission_uid) FROM analytics.submissions_enriched " +
                "WHERE template_uid = '%s') AS submissions",
            factsTable, templateUid);
        try {
            return jdbc.queryForObject(sql, (rs, rn) -> {
                long facts = rs.getLong("facts_rows");
                long subs = rs.getLong("submissions");
                double ratio = subs == 0 ? 1.0 : ((double) facts) / subs;
                ValidationResult vr = new ValidationResult();
                vr.passed(ratio >= 0.9);
                vr.factsRows(facts);
                vr.submissionCount(subs);
                vr.ratio(ratio);
                return vr;
            });
        } catch (Exception ex) {
            ValidationResult vr = new ValidationResult();
            vr.passed(false);
            vr.message(ex.getMessage());
            vr.factsRows(0);
            vr.submissionCount(0);
            vr.ratio(0.0);
            return vr;
        }
    }
}
