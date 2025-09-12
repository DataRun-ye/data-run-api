package org.nmcpye.datarun.analytics.dto;

import lombok.Builder;

/**
 * Simple sort DTO.
 * <pre>
 * Fields:
 *  - field (String): alias or MV column name to sort by.
 *  - desc (boolean): true => DESC, false => ASC.
 *
 * Notes:
 *  - Sorting prefers measure alias (e.g. "SUM_VAL") first; fallback to MV columns (grouped fields).
 *  - If grouping is used, ORDER BY must reference grouped columns or aggregated aliases; otherwise the DB will require grouping or aggregates (the builder should add a deterministic tie-breaker if necessary).
 *
 * </pre>
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Builder
public record SortDto(String fieldOrAlias,
                      boolean desc) {
}
