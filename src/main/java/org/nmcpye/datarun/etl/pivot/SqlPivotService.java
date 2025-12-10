package org.nmcpye.datarun.etl.pivot;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * General pivoting service: builds facts_wide table per template and populates it.
 *
 * Assumptions (explicit):
 * - tables exist: pivot.events_enriched, pivot.data_values_enriched, public.canonical_element
 * - canonical_element rows for a template include: canonical_element_id, safe_name, semantic_type, data_type, is_multiselect (boolean)
 * - event_id is canonical id (submission uid || repeat instance id)
 *
 * Usage: inject this service and call buildForTemplate(templateUid) or buildForAllTemplates()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlPivotService {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    // ---- public API ----

    public void buildForAllTemplates() {
        List<String> templates = jdbc.query(
            "SELECT DISTINCT template_uid FROM pivot.events_enriched",
            (rs, rowNum) -> rs.getString("template_uid")
        );
        log.info("Found {} templates to build", templates.size());
        for (String t : templates) {
            try {
                buildForTemplate(t);
            } catch (Exception ex) {
                log.error("Failure building template {}: {}", t, ex.getMessage(), ex);
            }
        }
    }

    public void buildForTemplate(String templateUid) {
        // 1) load CE metadata for template
        List<CanonicalElement> ces = loadCanonicalElements(templateUid);
        if (ces.isEmpty()) {
            log.warn("No canonical elements for template {}", templateUid);
            return;
        }

        // 2) compute sanitized column names and SQL types
        Map<String, ColumnDef> columns = buildColumnDefs(ces);

        // 3) create table DDL
        String tableName = "pivot.facts_wide_template_" + templateUid;
        String createSql = buildCreateTableSql(tableName, columns);
        log.debug("CREATE DDL:\n{}", createSql);

        // 4) optionally drop & recreate (you may change this strategy)
        jdbc.execute(createSql);

        // 5) build and execute the pivot INSERT SELECT
        String insertSql = buildInsertSelectSql(tableName, templateUid, columns);
        log.debug("INSERT SQL (first 1000 chars):\n{}", insertSql.substring(0, Math.min(1000, insertSql.length())));
        jdbc.update(insertSql); // bulk populate

        // 6) add indexes (basic)
        addIndexes(tableName);
        log.info("Built and populated wide table {}", tableName);
    }

    // ---- helpers ----

    private List<CanonicalElement> loadCanonicalElements(String templateUid) {
        String sql = "SELECT id, safe_name, semantic_type, data_type " +
            "FROM public.canonical_element " +
            "WHERE template_uid = ? ORDER BY id";
        return jdbc.query(sql, (ResultSet rs, int rowNum) -> {
            CanonicalElement ce = new CanonicalElement();
            ce.setCanonicalElementId(rs.getString("canonical_element_id"));
            ce.setSafeName(rs.getString("safe_name"));
            ce.setSemanticType(rs.getString("semantic_type"));
            ce.setDataType(rs.getString("data_type"));
            ce.setMultiselect(rs.getBoolean("is_multiselect"));
            return ce;
        }, templateUid);
    }

    private Map<String, ColumnDef> buildColumnDefs(List<CanonicalElement> ces) {
        Map<String, ColumnDef> map = new LinkedHashMap<>();
        Set<String> usedNames = new HashSet<>();
        for (CanonicalElement ce : ces) {
            String base = sanitizeColumn(ce.getSafeName());
            String col = base;
            if (usedNames.contains(col)) {
                String suffix = "__" + shortHash(ce.getCanonicalElementId());
                int maxBase = 58;
                if (col.length() > maxBase) col = col.substring(0, maxBase);
                col = col + suffix;
            }
            usedNames.add(col);

            String sqlType = mapToSqlType(ce);
            ColumnDef def = new ColumnDef(col, ce.getSafeName(), sqlType, ce.isMultiselect());
            map.put(col, def);
        }
        return map;
    }

    private String buildCreateTableSql(String tableName, Map<String, ColumnDef> columns) {
        List<String> parts = new ArrayList<>();
        parts.add("event_id text PRIMARY KEY");      // canonical id (submission OR repeat)
        parts.add("parent_event_id text");
        parts.add("submission_uid text NOT NULL");
        parts.add("template_uid text NOT NULL");
        parts.add("submission_creation_time timestamptz");
        // pivot columns
        for (ColumnDef c : columns.values()) {
            parts.add(c.getName() + " " + c.getSqlType());
        }
        parts.add("created_at timestamptz DEFAULT now()");
        String body = parts.stream().collect(Collectors.joining(",\n  "));
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (\n  " + body + "\n);";
    }

    private String buildInsertSelectSql(String tableName, String templateUid, Map<String, ColumnDef> columns) {
        // identity columns in target
        String targetCols = "event_id, parent_event_id, submission_uid, template_uid, submission_creation_time, " +
            columns.values().stream().map(ColumnDef::getName).collect(Collectors.joining(", "));

        // build select expressions for pivot columns
        List<String> expressions = new ArrayList<>();
        expressions.add("ev.event_id");
        expressions.add("ev.parent_event_id");
        expressions.add("ev.submission_uid");
        expressions.add("ev.template_uid");
        expressions.add("ev.submission_creation_time");

        List<String> aggregates = new ArrayList<>();
        // collect multiselect names if any for jsonb_agg subqueries later
        Set<String> multiselectSafeNames = new HashSet<>();

        for (ColumnDef c : columns.values()) {
            if (c.isMultiselect()) {
                // we'll use a scalar subquery per event to build the jsonb array (distinct)
                String safe = escapeLiteral(c.getSafeName());
                String expr = "(SELECT jsonb_agg(DISTINCT dv2.value_text) " +
                    "FROM pivot.data_values_enriched dv2 " +
                    "WHERE dv2.event_id = ev.event_id AND dv2.safe_name = '" + safe + "') AS " + c.getName();
                aggregates.add(expr);
                multiselectSafeNames.add(c.getSafeName());
            } else {
                // pick the appropriate dv column and cast using MAX(CASE ...)
                String safe = escapeLiteral(c.getSafeName());
                String castExpr = buildCaseCastExpression(safe, c.getSqlType());
                aggregates.add(castExpr + " AS " + c.getName());
            }
        }
        expressions.addAll(aggregates);

        String selectList = expressions.stream().collect(Collectors.joining(",\n  "));
        // Build the main FROM / JOIN / GROUP BY pattern
        // We'll include dv in join so MAX(CASE...) works; multiselects are handled via subqueries above (so duplicates OK)
        String sql = "INSERT INTO " + tableName + " (" + targetCols + ")\n" +
            "SELECT\n  " + selectList + "\n" +
            "FROM pivot.events_enriched ev\n" +
            "LEFT JOIN pivot.data_values_enriched dv ON dv.event_id = ev.event_id\n" +
            "WHERE ev.template_uid = '" + escapeLiteral(templateUid) + "'\n" +
            "GROUP BY ev.event_id, ev.parent_event_id, ev.submission_uid, ev.template_uid, ev.submission_creation_time;";
        return sql;
    }

    private String buildCaseCastExpression(String safeName, String sqlType) {
        // pick appropriate source column from data_values_enriched
        // prefer value_text/value_number depending on sqlType mapping
        if (sqlType.equalsIgnoreCase("timestamp")) {
            return "MAX(CASE WHEN dv.safe_name = '" + escapeLiteral(safeName) + "' THEN dv.value_text END)::timestamptz";
        } else if (sqlType.equalsIgnoreCase("bigint")) {
            return "MAX(CASE WHEN dv.safe_name = '" + escapeLiteral(safeName) + "' THEN dv.value_number END)::bigint";
        } else if (sqlType.equalsIgnoreCase("boolean")) {
            return "(MAX(CASE WHEN dv.safe_name = '" + escapeLiteral(safeName) + "' THEN dv.value_text END))::boolean";
        } else if (sqlType.equalsIgnoreCase("jsonb")) {
            // should be handled separately as multiselect
            return "NULL";
        } else {
            // default to text
            return "MAX(CASE WHEN dv.safe_name = '" + escapeLiteral(safeName) + "' THEN dv.value_text END)";
        }
    }

    private void addIndexes(String tableName) {
        try {
            jdbc.execute("CREATE INDEX IF NOT EXISTS " + tableName.replace('.', '_') + "_template_uid_idx ON " + tableName + " (template_uid)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS " + tableName.replace('.', '_') + "_submission_time_idx ON " + tableName + " (submission_creation_time)");
        } catch (Exception e) {
            log.warn("Could not create indexes on {}: {}", tableName, e.getMessage());
        }
    }

    // ---- utility functions ----

    private String sanitizeColumn(String safeName) {
        if (safeName == null) return "col";
        String s = safeName.trim().toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9]+", "_");
        s = s.replaceAll("^_+|_+$", "");
        if (s.isEmpty()) s = "col";
        if (s.length() > 58) s = s.substring(0, 58);
        return s;
    }

    private String shortHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) { // 6 hex chars ~ low chance collision
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode()).substring(0, Math.min(6, Integer.toHexString(input.hashCode()).length()));
        }
    }

    private String escapeLiteral(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }

    private String mapToSqlType(CanonicalElement ce) {
        if (ce.isMultiselect()) return "jsonb";
        String st = Optional.ofNullable(ce.getSemanticType()).orElse("").toLowerCase(Locale.ROOT);
        String dt = Optional.ofNullable(ce.getDataType()).orElse("").toLowerCase(Locale.ROOT);
        if (st.contains("date") || dt.contains("date") || dt.contains("datetime") || st.contains("datetime")) return "timestamptz";
        if (st.contains("number") || dt.contains("number") || dt.contains("int") || dt.contains("bigint")) return "bigint";
        if (st.contains("boolean") || dt.contains("boolean")) return "boolean";
        return "text";
    }

    // ---- small DTOs ----

    @Data
    public static class CanonicalElement {
        private String canonicalElementId;
        private String safeName;
        private String semanticType;
        private String dataType;
        private boolean multiselect;
        public boolean isMultiselect() { return Objects.equals(semanticType, SemanticType.MultiSelectOption.name()); }
    }

    @Getter
    public static class ColumnDef {
        private final String name;
        private final String safeName;
        private final String sqlType;
        private final boolean multiselect;
        public ColumnDef(String name, String safeName, String sqlType, boolean multiselect) {
            this.name = name; this.safeName = safeName; this.sqlType = sqlType; this.multiselect = multiselect;
        }
    }
}
