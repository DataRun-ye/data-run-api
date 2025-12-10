package org.nmcpye.datarun.analytics.domaintabletoolkit.pivot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("UnnecessaryLocalVariable")
@Component
@RequiredArgsConstructor
public class SqlGenerator {

    private static final Locale LOCALE = Locale.ROOT;
    private final Naming naming;

    private String wrap(String s) {
        // sanitize/normalize identifier to safe column name
        if (s == null) return null;
        return s.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(LOCALE);
    }

    /**
     * Create a wide table pivoting top-level (non-repeat) events for an activity.
     * Writes to {baseFq}_new.
     */
    public String buildSubmissionCreateSqlForBase(String baseFq, String activityId, List<CanonicalElement> ces) {
        String tableNew = Naming.newName(baseFq);


        // top-level CEs: parentRepeatId == null
        List<CanonicalElement> rootCes = ces.stream()
            .filter(c -> c.getParentRepeatId() == null)
            .toList();

        LinkedHashMap<String, String> exprs = new LinkedHashMap<>();
        for (CanonicalElement c : rootCes) {
            String base = Optional.ofNullable(c.getSafeName()).filter(s -> !s.isBlank()).orElse(c.getCanonicalElementId());
            String alias = wrap(base);
            exprs.put(alias, getExpression(c, alias));
        }

        String pivotCols = String.join(",\n    ", exprs.values());

        if (exprs.isEmpty()) {
            // create empty table with the dims but no pivoted columns
            String emptySql = String.format("DROP TABLE IF EXISTS %1$s; CREATE TABLE %1$s AS SELECT %2$s WHERE false;", tableNew, rootCes);
            return emptySql;
        }

        // dimensions / group by keys
        String dims = String.join(", ",
            "e.event_id",
            "e.parent_event_id",
            "e.event_ce_id",
            "e.template_uid",
            "e.submission_uid",
            "e.submission_creation_time",
            "e.event_type",
            "e.event_name",
            "e.assigned_assignment_uid",
            "e.activity_uid",
            "e.assigned_team_uid",
            "e.assigned_org_unit_uid",
            "e.anchor_ce_id",
            "e.anchor_semantic_type",
            "e.anchor_data_type",
            "e.anchor_name",
            "e.anchor_option_set_uid",
            "e.anchor_value_text",
            "e.anchor_ref_uid",
            "e.anchor_resolved_label"
        );

        String sql = String.format("""
                DROP TABLE IF EXISTS %1$s;
                CREATE TABLE %1$s AS
                SELECT
                  %2$s,
                  %3$s
                FROM analytics.events_enriched e
                LEFT JOIN analytics.data_values_enriched dv
                    ON (dv.event_id = e.event_id OR dv.repeat_instance_id = e.event_id)
                WHERE e.event_type IN ('root', 'submission') AND e.activity_uid = '%4$s'
                GROUP BY
                  %2$s;
                """,
            tableNew,   // %1$s
            dims,       // %2$s
            pivotCols,  // %3$s
            activityId  // %4$s
        );

        return sql;
    }

    /**
     * Create a wide table pivoting repeat-child events for an activity.
     * Writes to {baseFq}_new (usually baseFq ends with _repeats)
     */
    public String buildRepeatsCreateSqlForBase(String baseFq, String activityId, List<CanonicalElement> ces) {
        String newFq = Naming.newName(baseFq);
        String tableNew = baseFq + newFq;

        // child CEs: parentRepeatId != null
        List<CanonicalElement> childCes = ces.stream()
            .filter(c -> c.getParentRepeatId() != null)
            .toList();

        LinkedHashMap<String, String> exprs = new LinkedHashMap<>();
        for (CanonicalElement c : childCes) {
            String base = Optional.ofNullable(c.getSafeName()).filter(s -> !s.isBlank()).orElse(c.getCanonicalElementId());
            String alias = wrap(base);
            exprs.put(alias, getExpression(c, alias));
        }

        String pivotCols = String.join(",\n    ", exprs.values());

        // dimensions / group by keys for repeat events
        String dims = String.join(", ",
            "e.event_id",
            "e.parent_event_id",
            "e.event_ce_id",
            "e.template_uid",
            "e.submission_uid",
            "e.submission_creation_time",
            "e.event_type",
            "e.event_name",
            "e.assigned_assignment_uid",
            "e.activity_uid",
            "e.assigned_team_uid",
            "e.assigned_org_unit_uid",
            "e.anchor_ce_id",
            "e.anchor_semantic_type",
            "e.anchor_data_type",
            "e.anchor_name",
            "e.anchor_option_set_uid",
            "e.anchor_value_text",
            "e.anchor_ref_uid",
            "e.anchor_resolved_label",
            "dv.repeat_instance_id",
            "dv.parent_repeat_id",
            "dv.repeat_index"
        );

        String sql = String.format("""
                DROP TABLE IF EXISTS %1$s;
                CREATE TABLE %1$s AS
                SELECT
                  %2$s,
                  %3$s
                FROM analytics.events_enriched e
                JOIN analytics.data_values_enriched dv
                  ON  dv.event_id = e.event_id
                WHERE e.event_type = 'repeat' AND e.activity_uid = '%4$s'
                GROUP BY
                  %2$s;
                """,
            tableNew,   // %1$s
            dims,       // %2$s
            pivotCols,  // %3$s
            activityId  // %4$s
        );

        return sql;
    }

    /**
     * Creates SQL that writes to {baseFq}_new (baseFq must include schema, e.g. analytics.fact_mytemplate)
     */
    public String buildTemplateCreateSqlForBase(String baseFq, String templateUid, List<CanonicalElement> ces) {
        String tableNew = Naming.newName(baseFq);

        // dims (event-level) — adjust to exactly the columns present in events_enriched
        String dims = String.join(", ",
            "e.event_id",
            "e.parent_event_id",
            "e.event_ce_id",
            "e.template_uid",
            "e.submission_uid",
            "e.submission_creation_time",
            "e.event_type",
            "e.event_name",
            "e.assigned_assignment_uid",
            "e.activity_uid",
            "e.assigned_team_uid",
            "e.assigned_org_unit_uid",
            "e.anchor_ce_id",
            "e.anchor_semantic_type",
            "e.anchor_data_type",
            "e.anchor_name",
            "e.anchor_option_set_uid",
            "e.anchor_value_text",
            "e.anchor_ref_uid",
            "e.anchor_resolved_label"
        );

        LinkedHashMap<String, String> exprs = new LinkedHashMap<>();
        for (CanonicalElement c : ces) {
            String base = Optional.ofNullable(c.getSafeName()).filter(s -> !s.isBlank()).orElse(c.getCanonicalElementId());
            String alias = wrap(base); // wrap() already defined in your SqlGenerator
            exprs.put(alias, getExpression(c, alias));
        }
        String pivotCols = String.join(",\n    ", exprs.values());

        if (exprs.isEmpty()) {
            // create empty table with the dims but no pivoted columns
            String emptySql = String.format("DROP TABLE IF EXISTS %1$s; CREATE TABLE %1$s AS SELECT %2$s WHERE false;",
                tableNew, dims);
            return emptySql;
        }

        String sql = String.format("""
                DROP TABLE IF EXISTS %1$s;
                CREATE TABLE %1$s AS
                WITH events AS (
                  SELECT
                    e.event_id,
                    e.parent_event_id,
                    e.event_ce_id,
                    e.template_uid,
                    e.submission_uid,
                    e.submission_creation_time,
                    e.event_type,
                    e.event_name,
                    e.assigned_assignment_uid,
                    e.activity_uid,
                    e.assigned_team_uid,
                    e.assigned_org_unit_uid,
                    e.anchor_ce_id,
                    e.anchor_semantic_type,
                    e.anchor_data_type,
                    e.anchor_name,
                    e.anchor_option_set_uid,
                    e.anchor_value_text,
                    e.anchor_ref_uid,
                    e.anchor_resolved_label
                  FROM analytics.events_enriched e
                  WHERE e.template_uid = '%2$s'
                )
                SELECT
                  %3$s,
                  %4$s
                FROM events e
                LEFT JOIN analytics.data_values_enriched dv
                    ON dv.event_id = e.event_id
                GROUP BY
                  %3$s;
                """,
            tableNew,       // %1$s
            templateUid,    // %2$s
            dims,           // %3$s
            pivotCols       // %4$s
        );

        return sql;
    }


    /**
     * Generate pivot expression(s) for a canonical element.
     * Returns either a single expression or multiple comma-separated expressions
     * (for ref-types we emit label + uid (+ option_set) columns).
     * `alias` must already be sanitized (wrap()).
     */
    private static String getExpression(CanonicalElement c, String alias) {
        String ceId = Optional.ofNullable(c.getCanonicalElementId()).orElse("");
        String dt = Optional.ofNullable(c.getDataType()).orElse("").toLowerCase(Locale.ROOT);
        String st = Optional.ofNullable(c.getSemanticType()).orElse("").toLowerCase(Locale.ROOT);

        // ref types -> label and uid (and option_set_uid for option)
        if (!st.isEmpty() && (st.equals("option") || st.equals("team") || st.equals("orgunit")
            || st.equals("org_unit") || st.equals("activity") || st.equals("assignment"))) {

            String labelExpr = String.format(
                "MAX(CASE WHEN dv.canonical_element_id = '%s' THEN COALESCE(dv.resolved_ref_label, dv.value_text) END) AS \"%s\"",
                ceId, alias);

            String uidCol = alias + "_" + (st.equals("orgunit") || st.equals("org_unit") ? "ou" : st) + "_uid";
            String uidExpr = String.format(
                "MAX(CASE WHEN dv.canonical_element_id = '%s' THEN dv.value_ref_uid END) AS \"%s\"",
                ceId, uidCol);

            if ("option".equals(st)) {
                String optionSetExpr = String.format(
                    "MAX(CASE WHEN dv.canonical_element_id = '%s' THEN dv.ref_option_set_uid END) AS \"%s_option_set_uid\"",
                    ceId, alias);
                return labelExpr + ",\n    " + uidExpr + ",\n    " + optionSetExpr;
            } else {
                return labelExpr + ",\n    " + uidExpr;
            }
        }

        // JSON / ARRAY: if semantic type is repeat -> jsonb_agg, else single-value MAX-cast
        if ("json".equals(dt) || "array".equals(dt)) {
            if ("repeat".equals(st)) {
                // collect all json values into an array
                return String.format(
                    "COALESCE(jsonb_agg(DISTINCT dv.value_json) FILTER (WHERE dv.canonical_element_id = '%s'), '[]'::jsonb) AS \"%s\"",
                    ceId, alias);
            } else {
                // single-valued: aggregate text and cast back to jsonb
                return String.format(
                    "(MAX(CASE WHEN dv.canonical_element_id = '%s' THEN dv.value_json::text END))::jsonb AS \"%s\"",
                    ceId, alias);
            }
        }

        // non-ref scalar types -> single expression
        return switch (dt) {
            case "integer", "int", "numeric", "decimal", "float", "double" ->
                String.format("MAX(CASE WHEN dv.canonical_element_id = '%s' THEN dv.value_number END) AS \"%s\"", ceId, alias);
            case "boolean", "bool" ->
                String.format("MAX(CASE WHEN dv.canonical_element_id = '%s' THEN dv.value_bool END) AS \"%s\"", ceId, alias);
            case "timestamp", "date", "timestamptz" ->
                String.format("MAX(CASE WHEN dv.canonical_element_id = '%s' THEN dv.value_text END) AS \"%s\"", ceId, alias);
            default ->
                String.format("MAX(CASE WHEN dv.canonical_element_id = '%s' THEN COALESCE(dv.resolved_ref_label, dv.value_text) END) AS \"%s\"", ceId, alias);
        };
    }

}
