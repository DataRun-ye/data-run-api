package org.nmcpye.datarun.jpa.pivot;

import org.jooq.Field;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to provide access to pivot_grid_facts table fields and to whitelist allowed column names.
 * for security and preventing SQL injection.
 *
 * @author Hamza Assada 23/08/2025 (7amza.it@gmail.com)
 */
public final class PivotGridFactsColumns {

    public static final PivotGridFacts PG = PivotGridFacts.PIVOT_GRID_FACTS;

    // --- Define all allowed column names for the pivot grid ---
    // These strings must exactly match the column names in pivot_grid_facts materialized view.
    // Ensure add all relevant dimensions and measures here.
    private static final Set<String> ALLOWED_COLUMN_NAMES;
    private static final Map<String, Field<?>> COLUMN_TO_FIELD_MAP;

    static {
        // Collect all fields directly from the generated jOOQ table object
        Set<Field<?>> allFactsFields = Stream.of(
            PG.SUBMISSION_UID,
            PG.VALUE_ID,
            PG.TEAM_UID,
            PG.TEAM_UID,
            PG.ACTIVITY_UID,
            PG.FORM_TEMPLATE_UID,
            PG.ETC_UID,
            PG.DE_UID,
            PG.DE_NAME,
            PG.DE_VALUE_TYPE,
            PG.OPTION_UID,
            PG.OPTION_NAME,
            PG.OPTION_CODE,
            PG.VALUE_NUM,
            PG.VALUE_TEXT,
            PG.VALUE_TS,
            PG.VALUE_BOOL,
            PG.VALUE_REF_UID,
            PG.DELETED_AT,
            PG.SUBMISSION_COMPLETED_AT // Example of a timestamp column
            // Add any other relevant columns from your pivot_grid_facts MV
        ).collect(Collectors.toSet());

        ALLOWED_COLUMN_NAMES = allFactsFields.stream()
            .map(Field::getName).collect(Collectors.toUnmodifiableSet());

        COLUMN_TO_FIELD_MAP = Collections.unmodifiableMap(
            allFactsFields.stream()
                .collect(Collectors.toMap(Field::getName, Function.identity()))
        );
    }

    private PivotGridFactsColumns() {
        // Private constructor to prevent instantiation
    }

    /**
     * Checks if a given column name is allowed for pivot grid queries.
     *
     * @param columnName The name of the column.
     * @return true if the column is allowed, false otherwise.
     */
    public static boolean isAllowedColumn(String columnName) {
        return ALLOWED_COLUMN_NAMES.contains(columnName);
    }

    /**
     * Gets the jOOQ Field object for a given column name.
     * Throws an IllegalArgumentException if the column name is not allowed.
     *
     * @param columnName The name of the column.
     * @return The jOOQ Field object.
     */
    public static <T> Field<T> getField(String columnName) {
        if (!isAllowedColumn(columnName)) {
            throw new IllegalArgumentException("Column '" + columnName + "' is not an allowed column for pivot grid queries.");
        }
        // This cast is safe because we map from Field<?> to Map<String, Field<?>>
        // and then cast back to Field<T> when retrieved. jOOQ handles type safety well.
        @SuppressWarnings("unchecked")
        Field<T> field = (Field<T>) COLUMN_TO_FIELD_MAP.get(columnName);
        return field;
    }
}
