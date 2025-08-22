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

    public static final PivotGridFacts PIVOT_GRID_FACTS = PivotGridFacts.PIVOT_GRID_FACTS;

    // --- Define all allowed column names for the pivot grid ---
    // These strings must exactly match the column names in your pivot_grid_facts materialized view.
    // Ensure you add all relevant dimensions and measures here.
    private static final Set<String> ALLOWED_COLUMN_NAMES;
    private static final Map<String, Field<?>> COLUMN_TO_FIELD_MAP;

    static {
        // Collect all fields directly from the generated jOOQ table object
        Set<Field<?>> allFactsFields = Stream.of(
            PIVOT_GRID_FACTS.ID,
            PIVOT_GRID_FACTS.TEAM_ID,
            PIVOT_GRID_FACTS.TEAM_NAME,
            PIVOT_GRID_FACTS.PROJECT_ID,
            PIVOT_GRID_FACTS.PROJECT_NAME,
            PIVOT_GRID_FACTS.ACTIVATION_ID,
            PIVOT_GRID_FACTS.ACTIVATION_NAME,
            PIVOT_GRID_FACTS.SITE_ID,
            PIVOT_GRID_FACTS.SITE_NAME,
            PIVOT_GRID_FACTS.FORM_ID,
            PIVOT_GRID_FACTS.FORM_NAME,
            PIVOT_GRID_FACTS.ELEMENT_ID,
            PIVOT_GRID_FACTS.ELEMENT_NAME,
            PIVOT_GRID_FACTS.ELEMENT_TYPE,
            PIVOT_GRID_FACTS.VALUE_NUM,
            PIVOT_GRID_FACTS.VALUE_STR,
            PIVOT_GRID_FACTS.VALUE_DATE,
            PIVOT_GRID_FACTS.VALUE_BOOL,
            PIVOT_GRID_FACTS.VALUE_JSON,
            PIVOT_GRID_FACTS.CREATED_AT,
            PIVOT_GRID_FACTS.UPDATED_AT,
            PIVOT_GRID_FACTS.SUBMISSION_COMPLETED_AT // Example of a timestamp column
            // Add any other relevant columns from your pivot_grid_facts MV
        ).collect(Collectors.toSet());

        ALLOWED_COLUMN_NAMES = Collections.unmodifiableSet(
            allFactsFields.stream()
                .map(Field::getName)
                .collect(Collectors.toSet())
        );

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
