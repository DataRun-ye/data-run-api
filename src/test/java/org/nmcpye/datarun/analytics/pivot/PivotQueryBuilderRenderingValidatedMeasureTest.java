package org.nmcpye.datarun.analytics.pivot;

import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.analytics.pivot.PivotQueryBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Rendering-only tests using the real ValidatedMeasure record.
 * No Spring, no DB — uses jOOQ rendering (POSTGRES dialect).
 */
public class PivotQueryBuilderRenderingValidatedMeasureTest {

    private DSLContext dsl;
    private PivotQueryBuilder builder;

    @BeforeEach
    public void setUp() {
        dsl = DSL.using(SQLDialect.POSTGRES);
        builder = new PivotQueryBuilder(dsl);
    }

    @Test
    public void order_by_alias_then_dimension_is_rendered_using_validatedMeasure() {
        // dimension -> element_id
        List<String> dimensions = List.of("element_id");

        // Build ValidatedMeasure -> SUM(value_num) AS SUM_VAL
        var vm = ValidatedMeasure.builder()
            .elementId(null)
            .elementTemplateConfigId(null)
            .aggregation(ValidatedMeasure.MeasureAggregation.SUM)
            .targetField(org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS.VALUE_NUM) // typed Field<BigDecimal>
            .elementPredicate(DSL.trueCondition())
            .alias("SUM_VAL")
            .distinct(false)
            .optionId(null)
            .effectiveMode("TEMPLATE")
            .build();

        Select<?> sel = builder.buildSelect(
            dimensions,
            List.of(vm),
            null,
            null, null,
            List.of(new PivotQueryBuilder.Sort("SUM_VAL", true)),
            50, 0,
            null
        );

        String sql = dsl.renderInlined(sel).toUpperCase();
        assertThat(sql).contains("\"SUM_VAL\" DESC");
        // tie-breaker (group by dimension) should appear as an ORDER BY after alias
        assertThat(sql).contains("\"PUBLIC\".\"PIVOT_GRID_FACTS\".\"ELEMENT_ID\" ASC");
    }

    @Test
    public void order_by_measure_column_then_dimension_is_rendered_using_validatedMeasure() {
        List<String> dimensions = List.of("element_id");

        var vm = ValidatedMeasure.builder()
            .aggregation(ValidatedMeasure.MeasureAggregation.SUM)
            .targetField(org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS.VALUE_NUM)
            .elementPredicate(DSL.trueCondition())
            .alias("SUM_VAL")
            .distinct(false)
            .effectiveMode("TEMPLATE")
            .build();

        Select<?> sel = builder.buildSelect(
            dimensions,
            List.of(vm),
            null,
            null, null,
            List.of(new PivotQueryBuilder.Sort("value_num", true)),
            50, 0,
            null
        );

        String sql = dsl.renderInlined(sel).toUpperCase();
        assertThat(sql).contains("\"PUBLIC\".\"PIVOT_GRID_FACTS\".\"VALUE_NUM\" DESC");
        assertThat(sql).contains("\"PUBLIC\".\"PIVOT_GRID_FACTS\".\"ELEMENT_ID\" ASC");
    }
}
