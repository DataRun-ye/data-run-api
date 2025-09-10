//package org.nmcpye.datarun.analytics.pivot;
//
//import org.jooq.DSLContext;
//import org.jooq.SQLDialect;
//import org.jooq.Select;
//import org.jooq.conf.ParamType;
//import org.jooq.conf.SettingsTools;
//import org.jooq.impl.DSL;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
//import org.nmcpye.datarun.analytics.pivot.dto.FilterDto;
//import org.nmcpye.datarun.analytics.pivot.dto.SortDto;
//import org.nmcpye.datarun.analytics.pivot.model.ValidatedMeasure;
//import org.nmcpye.datarun.jooq.Tables;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//public class PivotQueryBuilderRenderSqlTest {
//
//    private DSLContext dsl;
//    private PivotQueryBuilder builder;
//
//    @BeforeEach
//    public void setUp() {
//
//        final var settings = SettingsTools.defaultSettings();
////        settings.setRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);
//        // Use the Postgres dialect for SQL rendering
//        this.dsl = DSL.using(SQLDialect.POSTGRES, settings);
//        this.builder = new PivotQueryBuilder(dsl);
//    }
//
//    @Test
//    public void render_filter_equals_numeric() {
//        Select<?> s = builder.buildSelect(
//            List.of("element_id"),
//            null,
//            List.of(new FilterDto("value_num", "=", "123")),
//            null, null,
//            null, 10, 0,
//            null
//        );
//
//        String sql = dsl.renderInlined(s).toUpperCase();
//        assertThat(sql).contains("\"VALUE_NUM\" = 123");
//        assertThat(sql).contains("GROUP BY");
//    }
//
//    @Test
//    public void render_filter_in_strings() {
//        var s = builder.buildSelect(
//            List.of("element_id"),
//            null,
//            List.of(new FilterDto("option_id", "IN", List.of("id-OPT1", "id-OPT2"))),
//            null, null,
//            null, 10, 0,
//            null
//        );
//
//        String sql1 = s.getSQL(ParamType.INLINED);
//        String sql = dsl.renderInlined(s).toUpperCase();
//        assertThat(sql).contains("\"OPTION_ID\" IN");
//        assertThat(sql).contains("'ID-OPT1'");
//        assertThat(sql).contains("'ID-OPT2'");
//    }
//
//    @Test
//    public void render_order_by_alias_and_field() {
//        // Build a simple aggregate measure to create an alias.
//        // We create a minimal ValidatedMeasure using a small local class for test
//        ValidatedMeasure vm = ValidatedMeasure.builder()
//            .alias("sum_val")
//            .targetField(Tables.PIVOT_GRID_FACTS.VALUE_NUM)
//            .aggregation(Aggregation.SUM)
////            .elementPredicate(Tables.PIVOT_GRID_FACTS.element_template_config_id.eq(123L))
//            .distinct(false).build();
//
//        Select<?> s = builder.buildSelect(
//            List.of("element_id"),
//            List.of(vm),
//            null,
//            null, null,
//            List.of(new SortDto("sum_val", true), new SortDto("element_id", false)),
//            50, 0,
//            null
//        );
//
//        String sql = dsl.renderInlined(s).toUpperCase();
//        // alias present in select list
//        assertThat(sql).contains("AS \"SUM_VAL\"");
//        // ORDER BY alias and field
//        assertThat(sql).contains("ORDER BY");
//        assertThat(sql).contains("\"SUM_VAL\" DESC");
//        assertThat(sql).contains("\"ELEMENT_ID\" ASC");
//    }
//
//    @Test
//    public void render_like_and_boolean_filter() {
//        Select<?> s = builder.buildSelect(
//            List.of("element_id"),
//            null,
//            List.of(
//                new FilterDto("value_text", "LIKE", "%test%"),
//                new FilterDto("value_bool", "=", true)
//            ),
//            null, null,
//            null, 100, 0,
//            null
//        );
//
//        String sql = dsl.renderInlined(s).toUpperCase();
//        assertThat(sql).contains("VALUE_TEXT");
//        assertThat(sql).contains("LIKE");
//        assertThat(sql).contains("\"VALUE_BOOL\" = TRUE");
//    }
//
//    @Test
//    public void render_range_and_acl_filter() {
//        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
//        LocalDateTime to = LocalDateTime.of(2025, 12, 31, 23, 59);
//
//        Select<?> s = builder.buildSelect(
//            List.of("element_id"),
//            null,
//            null,
//            from, to,
//            null, 25, 0,
//            Set.of("team-1", "team-2")
//        );
//
//        String sql = dsl.renderInlined(s).toUpperCase();
//        // time range and team filter present
//        assertThat(sql).contains("SUBMISSION_COMPLETED_AT");
//        assertThat(sql).contains("BETWEEN");
//        assertThat(sql).contains("\"TEAM_ID\" IN");
//    }
//}
//
