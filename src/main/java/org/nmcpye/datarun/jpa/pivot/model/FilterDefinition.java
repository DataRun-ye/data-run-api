package org.nmcpye.datarun.jpa.pivot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Hamza Assada 23/08/2025 (7amza.it@gmail.com)
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Operator {
        EQ, NEQ, GT, GTE, LT, LTE, LIKE, ILIKE, IN, NOT_IN, IS_NULL, IS_NOT_NULL, BETWEEN, NOT_BETWEEN
    }

    /**
     * The column from pivot_grid_facts to filter on.
     */
    private String column;

    /**
     * The operator to apply (e.g., EQ, GT, IN).
     */
    private Operator operator;

    /**
     * The value(s) to filter against.
     * Can be a single value for EQ, GT, etc., or a list of values for IN.
     * For BETWEEN, it should be a list of two values [start, end].
     * For date/time columns, this should be Instant.
     */
    private Object value;
}
