//package org.nmcpye.datarun.analytics.domaintabletoolkit;
//
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.dao.EmptyResultDataAccessException;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
///**
// * The orchestrator and the public-facing entry point for generating domain wide table.
// * It coordinates the other services to perform the full regeneration workflow
// * for a project's wide view.
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @see WideViewMetadataService
// * @see WideViewDdlBuilderService
// * @since 24/08/2025
// */
//@Service
//@Slf4j
//@AllArgsConstructor
//public class WideViewManagerService {
//
//    private final JdbcTemplate jdbcTemplate;
//    private static final String VIEW_NAME = "pivot_grid_facts_wide";
//
//    /**
//     * This is the main orchestration method. It can be called on a schedule,
//     * on application startup, or via an admin endpoint.
//     * Event-Driven: we could trigger manageView() in response to
//     * an ElementCreatedEvent or TemplateUpdatedEvent.
//     * This would provide faster schema updates.
//     */
//    public void manageView(String targetDdl) {
//        log.info("Starting management cycle for materialized view: {}", VIEW_NAME);
//
//        String currentDdl = getCurrentViewDdl();
//
//        // 3. Compare and act
//        if (currentDdl == null) {
//            log.warn("View {} does not exist. Performing initial creation.", VIEW_NAME);
//            rebuildView(targetDdl);
//        } else if (!areDdlsEquivalent(targetDdl, currentDdl)) {
//            log.warn("Schema drift detected for view {}. Rebuilding.", VIEW_NAME);
//            rebuildView(targetDdl);
//        } else {
//            log.info("Schema for view {} is up-to-date. Performing concurrent refresh.", VIEW_NAME);
//            refreshView();
//        }
//    }
//
//    private void rebuildView(String targetDdl) {
//        final String newViewName = VIEW_NAME + "_new";
//        final String oldViewName = VIEW_NAME + "_old";
//
//        log.info("Executing zero-downtime rebuild for view: {}", VIEW_NAME);
//
//        // Step 1: Create the new view with a different name. This is the long-running,
//        // non-blocking part. Users can continue querying the old view without interruption.
//        log.info("Step 1/4: Creating new view '{}' in the background.", newViewName);
//        jdbcTemplate.execute("DROP MATERIALIZED VIEW IF EXISTS " + newViewName + ";");
//
//        // Replace the hardcoded view name in the DDL with our temporary name
//        String newViewDdl = targetDdl.replaceFirst(VIEW_NAME, newViewName);
//        jdbcTemplate.execute(newViewDdl);
//        log.info("New view created successfully.");
//
//        // Step 2: Create any necessary indexes on the new view. This is also non-blocking.
//        log.info("Step 2/4: Creating indexes on new view '{}'.", newViewName);
//        jdbcTemplate.execute(String.format(
//                "CREATE UNIQUE INDEX %s_unique_idx ON %s (submission_id, COALESCE(repeat_instance_id, ''));",
//                newViewName, newViewName
//        ));
//        // Add other performance indexes here...
//        log.info("Indexes created successfully.");
//
//
//        // Step 3: Perform the atomic swap. This is extremely fast (milliseconds).
//        // The transaction ensures that the renames happen together or not at all.
//        log.info("Step 3/4: Performing atomic swap within a transaction.");
//        jdbcTemplate.execute(
//                "BEGIN;" +
//                        // Drop any leftover old view from a previously failed run
//                        "DROP MATERIALIZED VIEW IF EXISTS " + oldViewName + ";" +
//                        // Rename the current, active view to "_old"
//                        "ALTER MATERIALIZED VIEW IF EXISTS " + VIEW_NAME + " RENAME TO " + oldViewName + ";" +
//                        // Rename the new, fully prepared view to become the active one
//                        "ALTER MATERIALIZED VIEW " + newViewName + " RENAME TO " + VIEW_NAME + ";" +
//                        "COMMIT;"
//        );
//        log.info("Atomic swap completed. New view is now live.");
//
//        // Step 4: Clean up the old view. This is done outside the main transaction.
//        // There is now no one querying the old view, so dropping it is safe.
//        log.info("Step 4/4: Cleaning up old view '{}'.", oldViewName);
//        jdbcTemplate.execute("DROP MATERIALIZED VIEW IF EXISTS " + oldViewName + ";");
//        log.info("Zero-downtime rebuild complete for view: {}", VIEW_NAME);
//    }
//
//    private void refreshView() {
//        // This command requires a unique index to exist on the view
//        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + VIEW_NAME + ";");
//    }
//
//
//    private String getCurrentViewDdl() {
//        // Query PostgreSQL's system catalog to get the current view definition
//        try {
//            return jdbcTemplate.queryForObject(
//                    "SELECT definition FROM pg_matviews WHERE matviewname = ?",
//                    String.class,
//                    VIEW_NAME
//            );
//        } catch (EmptyResultDataAccessException e) {
//            return null; // View doesn't exist
//        }
//    }
//
//    /**
//     * Creates a "signature" of a DDL string that is resilient to formatting
//     * and column order changes.
//     */
//    private static String generateDdlSignature(String ddl) {
//        // 1. Normalize the entire string to handle whitespace and casing
//        String normalizedDdl = ddl.toLowerCase()
//                .replaceAll("--.*", "")       // Remove SQL comments
//                .replaceAll("\\s+", " ") // Collapse all whitespace to single spaces
//                .trim();
//
//        // 2. Extract the most volatile part: the column definitions
//        // This regex captures the text between SELECT and FROM
//        Pattern pattern = Pattern.compile("select(.*?)from");
//        Matcher matcher = pattern.matcher(normalizedDdl);
//
//        if (!matcher.find()) {
//            // If the pattern fails, fallback to the normalized string. This shouldn't happen.
//            return normalizedDdl;
//        }
//
//        String columnPart = matcher.group(1);
//
//        // 3. Create an order-independent representation of the columns
//        List<String> columns = Arrays.stream(columnPart.split(","))
//                .map(String::trim)
//                .sorted() // Sort alphabetically to make order irrelevant
//                .collect(Collectors.toList());
//
//        // 4. Reconstruct the signature using the sorted columns and the rest of the DDL
//        String stableColumnPart = String.join(",", columns);
//        String signature = matcher.replaceFirst("select " + stableColumnPart + " from");
//
//        return signature;
//    }
//
//    private boolean areDdlsEquivalent(String targetDdl, String currentDdl) {
//        if (targetDdl == null || currentDdl == null) {
//            return false;
//        }
//
//        // Generate canonical signatures for comparison
//        String targetSignature = generateDdlSignature(targetDdl);
//        String currentSignature = generateDdlSignature(currentDdl);
//
//        return targetSignature.equals(currentSignature);
//    }
//
////    // Helper methods for sanitizeNameForColumn, mapValueTypeToColumn, areDdlsEquivalent...
////    private boolean areDdlsEquivalent(String targetDdl, String currentDdl) {
////        if (targetDdl == null || currentDdl == null) {
////            return false;
////        }
////        String canonicalTarget = canonicalize(targetDdl);
////        String canonicalCurrent = canonicalize(currentDdl);
////        return canonicalTarget.equals(canonicalCurrent);
////    }
//
//    /**
//     * Converts a SQL DDL string into a canonical format for comparison.
//     * This makes the comparison insensitive to whitespace, casing, and comments.
//     */
//    private String canonicalize(String ddl) {
//        if (ddl == null) return "";
//
//        return ddl
//                // Remove block comments /* ... */
//                .replaceAll("/\\*.*?\\*/", "")
//                // Remove line comments -- ...
//                .replaceAll("--.*", "")
//                // Convert to a single case
//                .toLowerCase()
//                // Replace all sequences of whitespace (spaces, tabs, newlines) with a single space
//                .replaceAll("\\s+", " ")
//                // Trim leading/trailing whitespace and remove trailing semicolon
//                .trim()
//                .replaceAll(";$", "");
//    }
//}
