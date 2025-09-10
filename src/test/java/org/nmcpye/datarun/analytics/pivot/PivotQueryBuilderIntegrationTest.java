//package org.nmcpye.datarun.analytics.pivot;
//
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//import org.jooq.DSLContext;
//import org.jooq.Record;
//import org.jooq.Result;
//import org.jooq.SQLDialect;
//import org.jooq.impl.DSL;
//import org.junit.jupiter.api.*;
//import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
//import org.nmcpye.datarun.analytics.pivot.dto.SortDto;
//import org.nmcpye.datarun.analytics.pivot.model.ValidatedMeasure;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//import javax.sql.DataSource;
//import java.math.BigDecimal;
//import java.sql.Connection;
//import java.sql.Statement;
//import java.util.List;
//import java.util.Locale;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Integration-style test using Testcontainers to verify query execution & ordering.
// *
// * @author Hamza Assada
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class PivotQueryBuilderIntegrationTest {
//
//    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2")
//        .withDatabaseName("testdb")
//        .withUsername("test")
//        .withPassword("test");
//
//    private DataSource ds;
//    private DSLContext dsl;
//    private PivotQueryBuilder builder;
//
//    @BeforeAll
//    public void startContainerAndPrepare() throws Exception {
//        PG.start();
//
//        HikariConfig cfg = new HikariConfig();
//        cfg.setJdbcUrl(PG.getJdbcUrl());
//        cfg.setUsername(PG.getUsername());
//        cfg.setPassword(PG.getPassword());
//        cfg.setMaximumPoolSize(2);
//        ds = new HikariDataSource(cfg);
//
//        dsl = DSL.using(ds, SQLDialect.POSTGRES);
//        builder = new PivotQueryBuilder(dsl);
//
//        // Create minimal pivot table used for the test.
//        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
//            st.execute("""
//                CREATE TABLE IF NOT EXISTS pivot_grid_facts (
//                  id serial PRIMARY KEY,
//                  value_id numeric,
//                  element_id varchar(100),
//                  value_num numeric,
//                  deleted_at timestamp
//                );
//                """);
//        }
//    }
//
//    @AfterAll
//    public void stopContainer() {
//        if (ds instanceof HikariDataSource) ((HikariDataSource) ds).close();
//        PG.stop();
//    }
//
//    @BeforeEach
//    public void cleanTable() throws Exception {
//        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
//            st.execute("TRUNCATE TABLE pivot_grid_facts");
//        }
//    }
//
//    @Test
//    public void endToEnd_order_by_alias_desc_returns_expected_top_element() {
//        // Insert rows: element el-A has total 150, el-1 has total 10
//        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
//            st.execute("INSERT INTO pivot_grid_facts (element_id, value_num) VALUES ('el-A', 100)");
//            st.execute("INSERT INTO pivot_grid_facts (element_id, value_num) VALUES ('el-A', 50)");
//            st.execute("INSERT INTO pivot_grid_facts (element_id, value_num) VALUES ('el-1', 10)");
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
//
//        // Build validated measure that aggregates value_num
//        var vm = ValidatedMeasure.builder()
//            .aggregation(Aggregation.SUM)
//            .targetField(org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS.VALUE_NUM) // jOOQ generated field works fine
//            .elementPredicate(DSL.trueCondition())
//            .alias("SUM_VAL")
//            .distinct(false)
//            .effectiveMode("TEMPLATE")
//            .build();
//
//        // Build and execute the query
//        var sel = builder.buildSelect(
//            List.of("element_id"),
//            List.of(vm),
//            null,
//            null, null,
//            List.of(new SortDto("SUM_VAL", true)),
//            10, 0,
//            null
//        );
//
//        Result<Record> rows = dsl.fetch(sel);
//
//        assertThat(rows).isNotEmpty();
//        // The group-by returns one record per element_id with SUM(value_num). First should be 'el-A'
//        String firstElement = rows.get(0).get("element_id", String.class);
//        assertThat(firstElement).isEqualTo("el-A");
//
//        // Also assert sums: find the SUM_VAL in record
//        // Note: the alias may or may not be present as a typed field; use get("SUM_VAL")
//        BigDecimal sumVal = rows.get(0).get("SUM_VAL", BigDecimal.class);
//        assertThat(sumVal).isEqualByComparingTo(new BigDecimal("150"));
//        String sql = dsl.renderInlined(sel).toUpperCase(Locale.ROOT);
//        assertThat(sql).contains("\"SUM_VAL\""); // alias present
//        // ensure tie-breaker appended (grouped query)
//        assertThat(sql).contains("MIN(");
//        assertThat(sql).contains("VALUE_ID");
//    }
//}
