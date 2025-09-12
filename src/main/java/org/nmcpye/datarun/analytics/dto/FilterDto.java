package org.nmcpye.datarun.analytics.dto;

import lombok.Builder;

/**
 * Simple filter DTO.
 * <pre>
 * Fields:
 * - field (String): either an MV column name ("team_uid", "value_num", "option_uid") or a measure alias (if allowed).
 * - op (String): =, !=, IN, >, <, >=, <=, LIKE, ILIKE.
 * - value (Object): RHS value or collection (for IN).
 *
 * Notes:
 * - When field references element id forms (etc:<uid> or de:<uid>) the system will convert that to the appropriate MV column and value.
 * - If field is an alias, the builder resolves alias -> expression if possible. Prefer using MV columns for filters to avoid ambiguity.
 * </pre>
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Builder
public record FilterDto(
    String field, // dimension or alias or fact column
    String op,    // =, !=, IN, >, <, >=, <=, LIKE, ILIKE
    Object value // scalar or list for IN
) {
}
