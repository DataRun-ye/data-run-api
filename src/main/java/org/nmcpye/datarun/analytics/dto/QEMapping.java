package org.nmcpye.datarun.analytics.dto;

import org.jooq.Field;
import org.nmcpye.datarun.jpa.pivot.query.Scope;

/**
 * Complete metadata for a field that can be used in a pivot query.
 * <p>
 * A simplified mapping for fields within the pivot_grid_facts materialized view.
 *
 * @param id        The public API identifier (e.g., "assignment.org_unit").
 * @param field     The type-safe jOOQ field to select or aggregate (e.g., ORG_UNIT.NAME).
 * @param dataType  The data type of the field.
 * @param scope,    The scope of the field
 *                  // * @param requiredJoins A list of joins required to access this field.
 * @param isMeasure Whether this field is typically used as a measure.
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */


public record QEMapping(
    String id,
    Field<?> field,
    DataType dataType,
    Scope scope, // The scope of the field
//    List<JoinInfo> requiredJoins,
    boolean isMeasure
) {
}


//public record QEMapping(
//    String id,
//    Field<?> dimensionField, // Field for GROUP BY and filtering (e.g., TEAM_ID, ELEMENT_ID)
//    Field<?> measureField,   // Field for aggregation (e.g., VALUE_NUM)
//    DataType dataType,
//    Condition condition // Optional condition for element-specific measures
//) {
//}
