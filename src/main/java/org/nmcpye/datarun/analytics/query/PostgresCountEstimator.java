package org.nmcpye.datarun.analytics.query;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Estimate row counts for a jOOQ Select.
 * <p>
 * Strategy (in order):
 * 1) If the SELECT references a single base table (non-derived, non-subselect) attempt a fast pg_class lookup.
 * 2) Otherwise (or if the fast lookup fails), run EXPLAIN (FORMAT JSON) <query> and extract the planner estimate.
 * <p>
 * Returns -1 when no estimate is available.
 *
 * @author Hamza Assada
 * @since 13/09/2025
 */
@Slf4j
@RequiredArgsConstructor
public class PostgresCountEstimator implements CountProvider {

    private final DSLContext dsl;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Return an estimated number of rows. -1 means unknown/unavailable.
     */
    public long estimate(QueryExecutionPlan plan) {
        var select = plan.getSelect();
        // 1) try quick table-based estimate
        try {
            Optional<TableRef> maybeTable = extractSingleTableFromSql(select);
            if (maybeTable.isPresent()) {
                TableRef t = maybeTable.get();
                Long fast = fastRelTuplesFor(t);
                if (fast != null && fast >= 0) {
                    return fast;
                }
                // otherwise fall through to EXPLAIN
            }
        } catch (Exception ex) {
            log.debug("Fast pg_class estimate failed: {}", ex.getMessage());
            // Fall back to EXPLAIN path
        }

        // 2) fallback to EXPLAIN (FORMAT JSON) and extract "Plan Rows"
        try {
            String sql = dsl.render(select); // keeps bind placeholders
            List<Object> binds = select.getBindValues();
            String explainSql = "EXPLAIN (FORMAT JSON) " + sql;
            String json = dsl.fetchValue(explainSql, String.class, binds.toArray()).toString();
            if (json == null) return -1L;

            JsonNode root = mapper.readTree(json);
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                JsonNode planNode = first.get("Plan");
                Long planRows = findPlanRows(planNode);
                if (planRows != null) return planRows;
            }
        } catch (Exception ex) {
            log.debug("EXPLAIN estimate failed: {}", ex.getMessage());
        }

        return -1L;
    }

    // Small value-holder for schema.table
    private static class TableRef {
        final String schema; // nullable
        final String table;

        TableRef(String schema, String table) {
            this.schema = schema;
            this.table = table;
        }
    }

    /**
     * Try to parse a single FROM table (schema optional) from the rendered SQL.
     * This is intentionally conservative: it returns empty if the FROM target looks like a derived table (starts with '(')
     * or if multiple tables are present. It's a simple, safe parser (not a full SQL parser) but avoids regex pitfalls.
     */
    private Optional<TableRef> extractSingleTableFromSql(SelectQuery<?> select) {
        String sql = dsl.render(select); // SQL with placeholders; we only inspect text tokens
        // find "from" token case-insensitive
        String lower = sql.toLowerCase(Locale.ROOT);
        int fromIdx = lower.indexOf(" from ");
        if (fromIdx < 0) {
            // try start-of-line FROM
            if (lower.startsWith("from ")) fromIdx = 0;
            else return Optional.empty();
        } else {
            fromIdx += 1; // keep index at 'from' start
        }

        int cursor = fromIdx + 5; // position after 'from '
        int len = sql.length();

        // skip whitespace
        while (cursor < len && Character.isWhitespace(sql.charAt(cursor))) cursor++;
        if (cursor >= len) return Optional.empty();

        char c = sql.charAt(cursor);
        if (c == '(') {
            // derived table / subselect => not eligible for fast pg_class lookup
            return Optional.empty();
        }

        // parse possibly quoted identifier or unquoted identifier (schema or table)
        String first = parseIdentifier(sql, cursor);
        if (first == null) return Optional.empty();
        cursor += identifierAdvance(sql, cursor);

        // skip whitespace
        while (cursor < len && Character.isWhitespace(sql.charAt(cursor))) cursor++;

        String schema = null;
        String table = null;

        if (cursor < len && sql.charAt(cursor) == '.') {
            // schema-qualified: first is schema, next is table
            schema = first;
            cursor++; // skip '.'
            while (cursor < len && Character.isWhitespace(sql.charAt(cursor))) cursor++;
            String second = parseIdentifier(sql, cursor);
            if (second == null) return Optional.empty();
            table = second;
            // done
        } else {
            // single identifier; treat as table
            table = first;
        }

        // Validate there are no obvious additional tables in the immediate FROM expression
        // Skip the table identifier and any immediate alias
        cursor += identifierAdvance(sql, cursor);
        while (cursor < len && Character.isWhitespace(sql.charAt(cursor))) cursor++;
        // If the next non-space char is a comma or JOIN keyword, there are multiple tables -> bail out
        if (cursor < len) {
            char nc = Character.toLowerCase(sql.charAt(cursor));
            if (nc == ',') return Optional.empty();
            // check for 'join' (next tokens)
            String after = sql.substring(cursor, Math.min(len, cursor + 6)).toLowerCase(Locale.ROOT);
            if (after.startsWith("join") || after.startsWith("inner ") || after.startsWith("left ") || after.startsWith("right ")) {
                return Optional.empty();
            }
        }

        // Normalize quoted names by removing outer quotes
        if (schema != null) schema = stripQuotes(schema);
        if (table != null) table = stripQuotes(table);
        if (table == null || table.isEmpty()) return Optional.empty();
        return Optional.of(new TableRef(schema, table));
    }

    /**
     * Return how many characters an identifier consumes starting from offset (handles quoted identifiers).
     */
    private int identifierAdvance(String sql, int offset) {
        if (offset >= sql.length()) return 0;
        if (sql.charAt(offset) == '"') {
            // find closing quote
            int i = offset + 1;
            while (i < sql.length()) {
                if (sql.charAt(i) == '"') {
                    // handle doubled quotes inside quoted identifier -> skip pairs
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '"') {
                        i += 2;
                        continue;
                    } else {
                        return i - offset + 1;
                    }
                }
                i++;
            }
            return sql.length() - offset; // unterminated quote: consume rest
        } else {
            int i = offset;
            while (i < sql.length()) {
                char ch = sql.charAt(i);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$') i++;
                else break;
            }
            return i - offset;
        }
    }

    /**
     * Parse an identifier at given offset. Returns the identifier string (with quotes included) or null.
     */
    private String parseIdentifier(String sql, int offset) {
        if (offset >= sql.length()) return null;
        if (sql.charAt(offset) == '"') {
            int i = offset + 1;
            StringBuilder sb = new StringBuilder();
            while (i < sql.length()) {
                char ch = sql.charAt(i);
                if (ch == '"') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '"') {
                        // escaped quote inside identifier -> append one quote and skip pair
                        sb.append('"');
                        i += 2;
                        continue;
                    } else {
                        // closing quote
                        return sb.toString();
                    }
                } else {
                    sb.append(ch);
                    i++;
                }
            }
            // unterminated quoted identifier - treat what's collected
            return sb.toString();
        } else {
            int i = offset;
            StringBuilder sb = new StringBuilder();
            while (i < sql.length()) {
                char ch = sql.charAt(i);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$') {
                    sb.append(ch);
                    i++;
                } else {
                    break;
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        return s;
    }

    /**
     * Fast lookup via pg_class (reltuples). Returns null when not available.
     */
    private Long fastRelTuplesFor(TableRef t) {
        try {
            if (t == null) return null;
            if (t.schema != null) {
                // Use schema-qualified lookup via pg_namespace
                String sql = "SELECT c.reltuples::bigint " +
                    "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE c.relname = ? AND n.nspname = ? LIMIT 1";
                Long v = (Long) dsl.fetchValue(sql, Long.class, t.table, t.schema);
                return v;
            } else {
                // no schema: try any table with this relname (best-effort)
                String sql = "SELECT reltuples::bigint FROM pg_class WHERE relname = ? LIMIT 1";
                Long v = (Long) dsl.fetchValue(sql, Long.class, t.table);
                return v;
            }
        } catch (Exception ex) {
            log.debug("pg_class lookup failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Recursively find the first Plan Rows value in the EXPLAIN JSON plan tree.
     * Returns null if not found.
     */
    private Long findPlanRows(JsonNode node) {
        if (node == null) return null;
        if (node.has("Plan Rows") && node.get("Plan Rows").isNumber()) {
            return node.get("Plan Rows").asLong();
        }
        // Some Postgres versions/format may use "Plan" wrapper or "Plans" array
        if (node.has("Plan")) {
            Long v = findPlanRows(node.get("Plan"));
            if (v != null) return v;
        }
        if (node.has("Plans") && node.get("Plans").isArray()) {
            for (JsonNode child : node.get("Plans")) {
                Long v = findPlanRows(child);
                if (v != null) return v;
            }
        }
        // Sometimes the top is already a plan node with nested children under "Plans"
        if (node.has("Node Type") && node.has("Plans")) {
            for (JsonNode child : node.get("Plans")) {
                Long v = findPlanRows(child);
                if (v != null) return v;
            }
        }
        return null;
    }
}
