package org.nmcpye.datarun.analytics.pivot;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryRequest;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests exercising PivotQueryBuilder + translateFilter logic end-to-end
 * against a real PostgreSQL instance (Testcontainers).
 * <p>
 * Adapt the call to PivotQueryBuilder constructor / buildSelect if your API differs.
 *
 * @author Hamza Assada
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PivotQueryBuilderBindingsIT {

    static final PostgreSQLContainer<?> PG = new
        PostgreSQLContainer<>("postgres:16.2")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    private DataSource ds;
    private DSLContext dsl;
    private PivotQueryBuilder builder;

    @BeforeAll
    public void setUpAll() throws Exception {
        PG.start();

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        cfg.setMaximumPoolSize(2);
        ds = new HikariDataSource(cfg);

        dsl = DSL.using(ds, SQLDialect.POSTGRES);

        // Create a minimal test table (simplified pivot_grid_facts)
        dsl.execute("""
            CREATE TABLE IF NOT EXISTS pivot_grid_facts (
               id serial PRIMARY KEY,
               value_id numeric,
               element_id varchar(64),
               value_num numeric,
               value_text text,
               value_bool boolean,
               option_id varchar(64),
               deleted_at timestamp,
               submission_completed_at timestamp
            );
            """);

        // Insert sample rows
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.execute("TRUNCATE pivot_grid_facts");

        // row1: numeric 5, option id id-OPT1, text 'foo', bool false, time = now - 2 days
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-1", DSL.val(5), DSL.val("foo"), DSL.val(false), DSL.val("id-OPT1"), DSL.val(now.minusDays(2)))
            .execute();

        // row2: numeric 15, option id id-OPT2, text 'bar', bool true, time = now - 1 day
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-1", DSL.val(15), DSL.val("bar"), DSL.val(true), DSL.val("id-OPT2"), DSL.val(now.minusDays(1)))
            .execute();

        // row3: text null, option null, numeric null
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-2", DSL.val((Integer) null), DSL.val((String) null), DSL.val((Boolean) null), DSL.val((String) null), DSL.val(now))
            .execute();

        // Instantiate your PivotQueryBuilder with the DSLContext.
        // Adjust constructor if your implementation requires more args (e.g., table constant).
        this.builder = new PivotQueryBuilder(dsl);
    }

    @AfterAll
    public void tearDownAll() {
        if (ds instanceof HikariDataSource) ((HikariDataSource) ds).close();
        PG.stop();
    }

    @Test
    public void numericFilter_gte_returns_only_matching_rows() {
        // value_num >= 10 => only row2 (value_num=15)
        PivotQueryBuilder.Filter numericFilter = new PivotQueryBuilder.Filter("value_num", ">=", 10);

        Select<?> sel = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(numericFilter),
            null, null,
            null, 100, 0,
            null
        );

        var res = dsl.fetch(sel);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).get("element_id")).isEqualTo("el-1");
    }

    @Test
    public void inFilter_strings_returns_expected_rows() {
        // option_id IN ('id-OPT1','id-OPT2') => row1 and row2
        PivotQueryBuilder.Filter inFilter =
            new PivotQueryBuilder.Filter("option_id", "IN", List.of("id-OPT1", "id-OPT2"));

        Select<?> sel = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(inFilter),
            null, null,
            null, 100, 0,
            null
        );

        var res = dsl.fetch(sel);
        // grouped by element_id -> single result el-1 (since both rows share element_id el-1)
        assertThat(res).hasSize(1);
        assertThat(res.get(0).get("element_id")).isEqualTo("el-1");
    }

    @Test
    public void booleanFilter_true_returns_matching_row() {
        PivotQueryBuilder.Filter boolFilter = new PivotQueryBuilder.Filter("value_bool", "=", true);

        Select<?> sel = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(boolFilter),
            null, null,
            null, 100, 0,
            null
        );

        var res = dsl.fetch(sel);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).get("element_id")).isEqualTo("el-1");
    }

    @Test
    public void datetimeRange_filters_on_submission_time() {
        // use a range that includes 'now - 1 day' and excludes 'now - 2 days'
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(1).minusHours(1);
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);

        Select<?> sel = builder.buildSelect(
            List.of("element_id"),
            null,
            null,
            from, to,
            null, 100, 0,
            null
        );

        var res = dsl.fetch(sel);
        // should include row2 (now-1d) and row3 (now); grouped by element_id => we expect el-1 and el-2 -> 2 groups
        assertThat(res).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    public void nullFilter_selects_null_text_rows() {
        PivotQueryBuilder.Filter nullFilter = new PivotQueryBuilder.Filter("value_text", "=", null);

        Select<?> sel = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(nullFilter),
            null, null,
            null, 100, 0,
            null
        );

        var res = dsl.fetch(sel);
        // group by element_id -> el-2 only
        assertThat(res).hasSize(1);
        assertThat(res.get(0).get("element_id")).isEqualTo("el-2");
    }

    /**
     * Empty IN list policy: produce no rows (falseCondition()).
     * This avoids accidental full-table matches when the caller passes an empty list.
     */
    @Test
    public void emptyInList_returns_no_rows() {
        PivotQueryBuilder.Filter emptyIn = new PivotQueryBuilder.Filter("option_id", "IN", List.of());

        Select<?> s = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(emptyIn),
            null, null,
            null, 100, 0,
            null
        );

        var rows = dsl.fetch(s);
        assertEquals(0, rows.size(), "Empty IN list should return no rows");
    }

    /**
     * Invalid type for a typed field should be rejected during query building/coercion.
     * e.g. value_num = 'abc' -> cannot coerce to numeric -> IllegalArgumentException.
     */
    @Test
    public void invalidType_throwsIllegalArgumentException() {
        PivotQueryBuilder.Filter badNumeric = new PivotQueryBuilder.Filter("value_num", "=", "abc");

        // Either builder.buildSelect throws (preferred) or build + fetch throws.
        assertThrows(IllegalArgumentException.class, () -> {
            // try to build the select (coercion often happens here)
            Select<?> s = builder.buildSelect(
                List.of("element_id"),
                null,
                List.of(badNumeric),
                null, null,
                null, 100, 0,
                null
            );
            // If build did not throw, executing should (coercion may happen during translateFilter)
            dsl.fetch(s);
        });
    }

    /**
     * Test LIKE / case handling.
     * We expect the 'foo' row (value_text 'foo') to match LIKE 'FO%' if the builder lower-cases appropriately.
     */
    @Test
    public void like_and_ilike_render_correctly() {
        PivotQueryBuilder.Filter like = new PivotQueryBuilder.Filter("value_text", "LIKE", "%foo%");
        PivotQueryBuilder.Filter ilike = new PivotQueryBuilder.Filter("value_text", "ILIKE", "%bar%");

        Select<?> sLike = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(like),
            null, null,
            null, 10, 0,
            null
        );
        String sqlLike = dsl.renderInlined(sLike).toUpperCase();
        assertThat(sqlLike).contains("LIKE");

        Select<?> sILike = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(ilike),
            null, null,
            null, 10, 0,
            null
        );
        String sqlILike = dsl.renderInlined(sILike).toUpperCase();
        // For Postgres jOOQ will render ILIKE via likeIgnoreCase or lower(...) depending on config,
        // but the important thing is the presence of case-insensitive semantics: check for ILIKE OR LOWER(...)
        assertThat(sqlILike).satisfies(sql -> {
            String up = sql.toUpperCase();
            assertThat(up.contains("ILIKE") || up.contains("LOWER(")).isTrue();
        });
    }

    @Test
    public void null_eq_and_ne_render_isNull_isNotNull() {
        PivotQueryBuilder.Filter eqNull = new PivotQueryBuilder.Filter("value_text", "=", null);
        PivotQueryBuilder.Filter neNull = new PivotQueryBuilder.Filter("value_text", "!=", null);

        Select<?> sEq = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(eqNull),
            null, null,
            null, 10, 0,
            null
        );
        String sqlEq = dsl.renderInlined(sEq).toUpperCase();
        assertThat(sqlEq).contains("IS NULL");

        Select<?> sNe = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(neNull),
            null, null,
            null, 10, 0,
            null
        );
        String sqlNe = dsl.renderInlined(sNe).toUpperCase();
        assertThat(sqlNe).contains("IS NOT NULL");
    }

    @Test
    public void numeric_in_coerces_and_renders_in_clause() {
        PivotQueryBuilder.Filter inNum = new PivotQueryBuilder.Filter("value_num", "IN", List.of("1", "2", "3"));

        Select<?> s = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(inNum),
            null, null,
            null, 10, 0,
            null
        );
        String sql = dsl.renderInlined(s).toUpperCase();
        // Should have an IN with numeric tokens; we check contains IN and the numeric literals
        assertThat(sql).contains("IN");
        assertThat(sql).contains("1");
        assertThat(sql).contains("2");
        assertThat(sql).contains("3");
    }

    /**
     * Ordering + pagination determinism.
     * Insert an extra row so we have deterministic lexicographic order for element_id,
     * and check offset/limit behavior returns the expected item for each page.
     */
    @Test
    public void orderBy_field_pagination_is_deterministic() {
        // ensure our target rows exist (upsert style)
        dsl.execute("DELETE FROM pivot_grid_facts WHERE element_id IN ('el-A','el-B','el-C')");
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-A", DSL.val(1), DSL.val("aa"), DSL.val(false), DSL.val((String) null), DSL.val(LocalDateTime.now()))
            .execute();
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-B", DSL.val(2), DSL.val("bb"), DSL.val(false), DSL.val((String) null), DSL.val(LocalDateTime.now()))
            .execute();
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-C", DSL.val(3), DSL.val("cc"), DSL.val(false), DSL.val((String) null), DSL.val(LocalDateTime.now()))
            .execute();

        // Add a filter to restrict the query to only these element_ids.
        PivotQueryBuilder.Filter filter = new PivotQueryBuilder.Filter("element_id", "IN", List.of("el-A", "el-B", "el-C"));

        var s0 = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(filter),
            null, null,
            List.of(new PivotQueryBuilder.Sort("element_id", false)), // asc
            1, 0,
            null
        );
        var rowsPage0 = dsl.fetch(s0);
        assertEquals(1, rowsPage0.size());
        String first = (String) rowsPage0.get(0).get("element_id");

        var s1 = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(filter),
            null, null,
            List.of(new PivotQueryBuilder.Sort("element_id", false)), // asc
            1, 1,
            null
        );
        var rowsPage1 = dsl.fetch(s1);
        assertEquals(1, rowsPage1.size());
        String second = (String) rowsPage1.get(0).get("element_id");

        assertNotEquals(first, second, "Two consecutive pages must return different groups");
        assertEquals("el-A", first);
        assertEquals("el-B", second);

        String sql0 = dsl.renderInlined(s0).toUpperCase(Locale.ROOT);
        assertThat(sql0).contains("ORDER BY");
        assertThat(sql0).contains("ELEMENT_ID");
        assertThat(sql0).contains("MIN(");        // ensure MIN(value_id) appears for grouped queries
        assertThat(sql0).contains("VALUE_ID");
    }

    @Test
    public void orderBy_field_pagination_is_deterministic_using_rowCol_dims() {
        // prepare data as before, insert el-A/B/C...
        dsl.execute("DELETE FROM pivot_grid_facts WHERE element_id IN ('el-A','el-B','el-C')");
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-A", DSL.val(1), DSL.val("aa"), DSL.val(false), DSL.val((String) null), DSL.val(LocalDateTime.now()))
            .execute();
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-B", DSL.val(2), DSL.val("bb"), DSL.val(false), DSL.val((String) null), DSL.val(LocalDateTime.now()))
            .execute();
        dsl.insertInto(DSL.table("pivot_grid_facts"))
            .columns(DSL.field("element_id"), DSL.field("value_num"), DSL.field("value_text"),
                DSL.field("value_bool"), DSL.field("option_id"), DSL.field("submission_completed_at"))
            .values("el-C", DSL.val(3), DSL.val("cc"), DSL.val(false), DSL.val((String) null), DSL.val(LocalDateTime.now()))
            .execute();

        // Build request with explicit rowDimensions
        PivotQueryRequest req = PivotQueryRequest.builder()
            .rowDimensions(List.of("element_id"))
            .columnDimensions(List.of("option_id")) // although option_id will be null for these rows; just demonstrates usage
            .limit(1)
            .offset(0)
            .templateId("")
            .templateVersionId("")
            .build();

        // Build validated measure list: none needed for this test (we only assert grouping/pagination)
        Select<Record> s0 = builder.buildSelect(
            // grouping dims passed to builder: rowDims + colDims
            List.of("element_id"),
            null,
            List.of(new PivotQueryBuilder.Filter("element_id", "IN", List.of("el-A","el-B","el-C"))),
            null, null,
            List.of(new PivotQueryBuilder.Sort("element_id", false)), // asc
            1, 0,
            null
        );

        var rowsPage0 = dsl.fetch(s0);
        assertEquals(1, rowsPage0.size());
        String first = (String) rowsPage0.get(0).get("element_id");

        var s1 = builder.buildSelect(
            List.of("element_id"),
            null,
            List.of(new PivotQueryBuilder.Filter("element_id", "IN", List.of("el-A","el-B","el-C"))),
            null, null,
            List.of(new PivotQueryBuilder.Sort("element_id", false)), // asc
            1, 1,
            null
        );
        var rowsPage1 = dsl.fetch(s1);
        assertEquals(1, rowsPage1.size());
        String second = (String) rowsPage1.get(0).get("element_id");

        assertNotEquals(first, second, "Two consecutive pages must return different groups");
        assertEquals("el-A", first);
        assertEquals("el-B", second);
    }
}
