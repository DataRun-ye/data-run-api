package org.nmcpye.datarun.jpa.pivot.query;

import org.jooq.Field;

import java.util.List;

/**
 * Complete metadata for a field that can be used in a pivot query.
 *
 * @param id            The public API identifier (e.g., "assignment.org_unit").
 * @param field         The type-safe jOOQ field to select or aggregate (e.g., ORG_UNIT.NAME).
 * @param dataType      The data type of the field.
 * @param scope,        The scope of the field
 * @param requiredJoins A list of joins required to access this field.
 * @param isMeasure     Whether this field is typically used as a measure.
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 * @see JoinInfo
 */
public record PivotableFieldMapping(
    String id,
    Field<?> field,
    PivotDataType dataType,
    Scope scope, // The scope of the field
    List<JoinInfo> requiredJoins,
    boolean isMeasure
) {
}
