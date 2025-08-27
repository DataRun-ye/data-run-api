package org.nmcpye.datarun.analytics.pivot;

import lombok.Builder;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * Result of validating a MeasureRequest.
 * <p>
 * - targetField: the jOOQ Field to aggregate (e.g. pivot_grid_facts.value_num)
 * - elementPredicate: a Condition restricting rows to that element/template/option
 * <p>
 * The targetField is typed (Field<T>) but exposed as Field<?> for generality.
 * Consumers must cast appropriately based on the aggregation type (e.g. SUM => Field<BigDecimal>).
 *
 * @param elementId               data_element.id (ULID) if available
 * @param elementTemplateConfigId etc.id (Long) if template-scoped and available
 * @param aggregation             enum
 * @param targetField             typed Field (BigDecimal, Boolean->CASE->Integer, String, LocalDateTime)
 * @param elementPredicate        condition scoping element/template + option if needed
 * @param alias                   An alias for the aggregated measure
 * @param distinct                distinct flag for COUNT
 * @param optionId
 * @param effectiveMode           "TEMPLATE" or "GLOBAL"
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Builder
public record ValidatedMeasure(
    String elementId,
    Long elementTemplateConfigId,
    MeasureAggregation aggregation,
    Field<?> targetField,
    Condition elementPredicate,
    String alias,
    boolean distinct,
    String optionId,
    String effectiveMode
) {
    public enum MeasureAggregation {
        SUM, AVG, MIN, MAX, COUNT, COUNT_DISTINCT, SUM_TRUE
    }
}
