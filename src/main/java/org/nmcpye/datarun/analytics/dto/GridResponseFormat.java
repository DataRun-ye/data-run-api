package org.nmcpye.datarun.analytics.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Output format hint for the query endpoint.
 * <p>
 * <pre>
 * Values:
 * - `TABLE_ROWS`: return a list of rows where each row contains the selected dimensions and measure columns.
 * - `PIVOT_MATRIX`: return a classic pivot matrix with rowDimension keys, columnDimension keys, and cell values (may be sparse).
 *
 * Note:
 * - `TABLE_ROWS` is the simpler representation (SELECT result), return raw Result<Record> mapped to JSON rows.
 * - `PIVOT_MATRIX` still fetch aggregated rows grouped by row & column dims, requires additional post-processing (server-side pivoting into a 2D matrix), grouping logic and possibly more memory.
 * </pre>
 *
 * @author Hamza Assada
 * @since 28/08/2025
 */
public enum GridResponseFormat {
    TABLE_ROWS,
    PIVOT_MATRIX;

    @JsonCreator
    public static GridResponseFormat forValue(String v) {
        if (v == null) return TABLE_ROWS;
        try {
            return GridResponseFormat.valueOf(v.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Default to TABLE_ROWS for unknown values
            return TABLE_ROWS;
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
