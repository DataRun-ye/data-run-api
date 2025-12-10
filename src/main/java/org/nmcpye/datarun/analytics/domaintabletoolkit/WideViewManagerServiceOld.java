package org.nmcpye.datarun.analytics.domaintabletoolkit;//package org.nmcpye.datarun.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.analytics.ElementMetadataService;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ActivityAnalyticsMetadata;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.CeMeta;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 24/08/2025
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WideViewManagerServiceOld {
    private final ElementMetadataService elementMetadataService;

    private final JdbcTemplate jdbcTemplate;
    private static final String VIEW_NAME = "pivot_grid_facts_wide";

    /**
     * This is the main orchestration method. It can be called on a schedule,
     * on application startup, or via an admin endpoint.
     * Event-Driven: we could trigger manageView() in response to
     * an ElementCreatedEvent or TemplateUpdatedEvent.
     * This would provide faster schema updates.
     */
    public void manageView(String targetDdl) {
        log.info("Starting management cycle for materialized view: {}", VIEW_NAME);

//        // 1. Generate the "ideal" DDL based on current metadata
//        String targetDdl = generateTargetDdl();

        // 2. Get the current DDL from the database
        String currentDdl = getCurrentViewDdl();

        // 3. Compare and act
        if (currentDdl == null) {
            log.warn("View {} does not exist. Performing initial creation.", VIEW_NAME);
            rebuildView(targetDdl);
        } else if (!areDdlsEquivalent(targetDdl, currentDdl)) {
            log.warn("Schema drift detected for view {}. Rebuilding.", VIEW_NAME);
            rebuildView(targetDdl);
        } else {
            log.info("Schema for view {} is up-to-date. Performing concurrent refresh.", VIEW_NAME);
            refreshView();
        }
    }

    private void rebuildView(String targetDdl) {
        final String newViewName = VIEW_NAME + "_new";
        final String oldViewName = VIEW_NAME + "_old";

        log.info("Executing zero-downtime rebuild for view: {}", VIEW_NAME);

        // Step 1: Create the new view with a different name. This is the long-running,
        // non-blocking part. Users can continue querying the old view without interruption.
        log.info("Step 1/4: Creating new view '{}' in the background.", newViewName);
        jdbcTemplate.execute("DROP MATERIALIZED VIEW IF EXISTS " + newViewName + ";");

        // Replace the hardcoded view name in the DDL with our temporary name
        String newViewDdl = targetDdl.replaceFirst(VIEW_NAME, newViewName);
        jdbcTemplate.execute(newViewDdl);
        log.info("New view created successfully.");

        // Step 2: Create any necessary indexes on the new view. This is also non-blocking.
        log.info("Step 2/4: Creating indexes on new view '{}'.", newViewName);
        jdbcTemplate.execute(String.format(
            "CREATE UNIQUE INDEX %s_unique_idx ON %s (submission_id, COALESCE(repeat_instance_id, ''));",
            newViewName, newViewName
        ));
        // Add other performance indexes here...
        log.info("Indexes created successfully.");


        // Step 3: Perform the atomic swap. This is extremely fast (milliseconds).
        // The transaction ensures that the renames happen together or not at all.
        log.info("Step 3/4: Performing atomic swap within a transaction.");
        jdbcTemplate.execute(
            "BEGIN;" +
                // Drop any leftover old view from a previously failed run
                "DROP MATERIALIZED VIEW IF EXISTS " + oldViewName + ";" +
                // Rename the current, active view to "_old"
                "ALTER MATERIALIZED VIEW IF EXISTS " + VIEW_NAME + " RENAME TO " + oldViewName + ";" +
                // Rename the new, fully prepared view to become the active one
                "ALTER MATERIALIZED VIEW " + newViewName + " RENAME TO " + VIEW_NAME + ";" +
                "COMMIT;"
        );
        log.info("Atomic swap completed. New view is now live.");

        // Step 4: Clean up the old view. This is done outside the main transaction.
        // There is now no one querying the old view, so dropping it is safe.
        log.info("Step 4/4: Cleaning up old view '{}'.", oldViewName);
        jdbcTemplate.execute("DROP MATERIALIZED VIEW IF EXISTS " + oldViewName + ";");
        log.info("Zero-downtime rebuild complete for view: {}", VIEW_NAME);
    }

    private void refreshView() {
        // This command requires a unique index to exist on the view
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + VIEW_NAME + ";");
    }

    private String generateTargetDdl(String activity) {
        ActivityAnalyticsMetadata ceMeta = elementMetadataService.getPivotableElements(activity);
        StringBuilder columnsBuilder = new StringBuilder();

        for (CeMeta element : ceMeta.elements()) {
            String columnName = canonicalize(element.columnAlias());
            String valueColumn = mapValueTypeToColumn(element); // "Number" -> "ev.value_num"

            columnsBuilder.append(String.format(
                ",\n    MAX(CASE WHEN ev.element_id = '%s' THEN %s END) AS %s",
                element.elementId(),
                valueColumn,
                columnName
            ));
        }

        // Using a template for the static part of the query is best practice
        return String.format(
            """
                CREATE MATERIALIZED VIEW %s AS
                SELECT
                    ri.id AS instance_key
                    sub.id AS submission_uid,
                    sub.assignment_uid,
                    sub.team_uid,
                    sub.org_unit_uid,
                    sub.activity_uid,
                    %s
                FROM
                    data_submission sub
                    LEFT JOIN repeat_instance ri ON sub.id = ri.submission_id
                    LEFT JOIN element_data_value ev ON sub.id = ev.submission_id AND COALESCE(ri.id, '') = COALESCE(ev.repeat_instance_id, '')
                WHERE
                    sub.deleted_at IS NULL
                GROUP BY
                    sub.id,
                    ri.id;
                """, VIEW_NAME, columnsBuilder
        );
    }

    private String mapValueTypeToColumn(CeMeta element) {
        final var semanticType = element.semanticType();
        if (semanticType != null) {
            if (semanticType.isRef()) {
                return switch (element.semanticType()) {
                    case OrgUnit, Team, Activity, Option -> "ev.value_ref_uid";
                    case MultiSelectOption -> "ev.value_json";
                    default -> "ev.value_text";
                };
            }

        }

        return switch (element.dataType()) {
            case INTEGER, DECIMAL -> "ev.value_number";
            default -> "ev.value_text";
        };
    }


    private String getCurrentViewDdl() {
        // Query PostgreSQL's system catalog to get the current view definition
        try {
            return jdbcTemplate.queryForObject(
                "SELECT definition FROM pg_matviews WHERE matviewname = ?",
                String.class,
                VIEW_NAME
            );
        } catch (EmptyResultDataAccessException e) {
            return null; // View doesn't exist
        }
    }

    /**
     * Creates a "signature" of a DDL string that is resilient to formatting
     * and column order changes.
     */
    private static String generateDdlSignature(String ddl) {
        // 1. Normalize the entire string to handle whitespace and casing
        String normalizedDdl = ddl.toLowerCase()
            .replaceAll("--.*", "")       // Remove SQL comments
            .replaceAll("\\s+", " ") // Collapse all whitespace to single spaces
            .trim();

        // 2. Extract the most volatile part: the column definitions
        // This regex captures the text between SELECT and FROM
        Pattern pattern = Pattern.compile("select(.*?)from");
        Matcher matcher = pattern.matcher(normalizedDdl);

        if (!matcher.find()) {
            // If the pattern fails, fallback to the normalized string. This shouldn't happen.
            return normalizedDdl;
        }

        String columnPart = matcher.group(1);

        // 3. Create an order-independent representation of the columns
        List<String> columns = Arrays.stream(columnPart.split(","))
            .map(String::trim)
            .sorted() // Sort alphabetically to make order irrelevant
            .collect(Collectors.toList());

        // 4. Reconstruct the signature using the sorted columns and the rest of the DDL
        String stableColumnPart = String.join(",", columns);
        String signature = matcher.replaceFirst("select " + stableColumnPart + " from");

        return signature;
    }

    private boolean areDdlsEquivalent(String targetDdl, String currentDdl) {
        if (targetDdl == null || currentDdl == null) {
            return false;
        }

        // Generate canonical signatures for comparison
        String targetSignature = generateDdlSignature(targetDdl);
        String currentSignature = generateDdlSignature(currentDdl);

        return targetSignature.equals(currentSignature);
    }

//    // Helper methods for sanitizeNameForColumn, mapValueTypeToColumn, areDdlsEquivalent...
//    private boolean areDdlsEquivalent(String targetDdl, String currentDdl) {
//        if (targetDdl == null || currentDdl == null) {
//            return false;
//        }
//        String canonicalTarget = canonicalize(targetDdl);
//        String canonicalCurrent = canonicalize(currentDdl);
//        return canonicalTarget.equals(canonicalCurrent);
//    }

    /**
     * Converts a SQL DDL string into a canonical format for comparison.
     * This makes the comparison insensitive to whitespace, casing, and comments.
     */
    private String canonicalize(String ddl) {
        if (ddl == null) return "";

        return ddl
            // Remove block comments /* ... */
            .replaceAll("/\\*.*?\\*/", "")
            // Remove line comments -- ...
            .replaceAll("--.*", "")
            // Convert to a single case
            .toLowerCase()
            // Replace all sequences of whitespace (spaces, tabs, newlines) with a single space
            .replaceAll("\\s+", " ")
            // Trim leading/trailing whitespace and remove trailing semicolon
            .trim()
            .replaceAll(";$", "");
    }
}
