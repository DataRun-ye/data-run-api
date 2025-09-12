package org.nmcpye.datarun.analytics.model;

import lombok.Builder;
import org.jooq.Condition;
import org.jooq.Field;
import org.nmcpye.datarun.analytics.dto.Aggregation;

/**
 * Result of validating a MeasureRequest and the canonical form used by the query builder.
 *
 * <p>
 * A ValidatedMeasure encapsulates everything needed to render a correct,
 * type-safe aggregate in SQL:
 * </p>
 *
 * <p>Important semantics:</p>
 * <ul>
 *   <li>Use {@link #elementPredicate} via {@code filterWhere(...)} so each aggregate can be scoped independently of the global WHERE.</li>
 *   <li>If the incoming client identifier is in the form {@code etc:<uid>} prefer {@code etc_uid} scoping. If it is {@code de:<uid>} use {@code de_uid} scoping.</li>
 *   <li>{@code targetField} must be correctly typed (e.g. Field<BigDecimal> for value_num) — this prevents jOOQ binding/type errors at runtime.</li>
 * </ul>
 *
 * @param deUid            — the DataElement UID (de.uid) when the measure is global, or null when template-scoped.
 * @param etcUid           the ElementTemplateConfig UID (etc.uid) when the measure is template-scoped; null for global measures.
 * @param aggregation      aggregation kind (SUM, AVG, COUNT, COUNT_DISTINCT, MIN, MAX, SUM_TRUE).
 * @param targetField      the jOOQ Field<?> instance that will be aggregated (e.g. PIVOT_GRID_FACTS.VALUE_NUM, PIVOT_GRID_FACTS.VALUE_TS, a CASE expr for SUM_TRUE, or OPTION_UID field).
 * @param elementPredicate jOOQ Condition scoping rows for this measure (e.g. PIVOT_GRID_FACTS.ETC_UID.eq('etc:...') or PIVOT_GRID_FACTS.DE_UID.eq('de:...')). This predicate is applied via {@code .filterWhere(elementPredicate)} on the aggregate.
 * @param alias            final SQL alias for the aggregated expression (must be unique in the SELECT list).
 * @param distinct         boolean flag used by COUNT to indicate COUNT(DISTINCT ...).
 * @param optionUid        optional, when a measure specifically targets a selected option (counts/sums restricted to a particular option value row).
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Builder
public record ValidatedMeasure(
    String deUid,
    String etcUid,
    Aggregation aggregation,
    Field<?> targetField,
    Condition elementPredicate,
    String alias,
    boolean distinct,
    String optionUid
) {
}
