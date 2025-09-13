package org.nmcpye.datarun.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client-side measure request (template-mode-first).
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasureRequest {
    /**
     * fieldId: client identifier for the element. Accepts:
     * <p>
     * - "etc:< uid >"   => template-scoped element (prefer etc_uid)
     * <p>
     * - "de:< uid >" or "<de_uid>" => global DataElement referenced by de_uid
     * <p>
     * - plain etc uid or de uid (resolver must handle)
     */
    private String fieldId;

    /**
     * aggregation: "SUM", "AVG", "COUNT", "COUNT_DISTINCT", "MIN", "MAX", "SUM_TRUE".
     */
    private String aggregation;

    /**
     * alias: optional column alias. If null, MeasureValidationService produces stable alias.
     */
    private String alias;

    /**
     * distinct: optional (for COUNT): whether to use DISTINCT.
     */
    private Boolean distinct;

    /**
     * optionId: optional (uid) used when measure targets a specific option (applies to scope predicate).
     */
    private String optionId;    // optional when counting per option (option ULID)
}
