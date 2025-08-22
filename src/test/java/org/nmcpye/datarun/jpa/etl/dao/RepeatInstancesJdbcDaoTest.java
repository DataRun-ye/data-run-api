//package org.nmcpye.datarun.jpa.etl.dao;
//
//
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//import javax.sql.DataSource;
//import java.time.Instant;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
///**
// * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class RepeatInstancesJdbcDaoTest {
//
//    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2")
//        .withDatabaseName("testdb").withUsername("test").withPassword("test");
//
//    private DataSource ds;
//    private NamedParameterJdbcTemplate namedJdbc;
//    private RepeatInstancesJdbcDao dao;
//    private JdbcTemplate jdbc; // for assertions
//
//    @BeforeAll
//    public void startContainerAndCreateSchema() {
//        PG.start();
//
//        HikariConfig cfg = new HikariConfig();
//        cfg.setJdbcUrl(PG.getJdbcUrl());
//        cfg.setUsername(PG.getUsername());
//        cfg.setPassword(PG.getPassword());
//        ds = new HikariDataSource(cfg);
//        namedJdbc = new NamedParameterJdbcTemplate(ds);
//        jdbc = new JdbcTemplate(ds);
//        dao = new RepeatInstancesJdbcDao(namedJdbc); // adapt constructor if needed
//
//        // run DDL
//        String ddl = """
//            CREATE TABLE repeat_instance (
//              id varchar(26) PRIMARY KEY,
//              submission_id varchar(50) NOT NULL,
//              repeat_path varchar(3000) NOT NULL,
//              repeat_index bigint,
//              client_updated_at timestamp,
//              deleted_at timestamp,
//              created_date timestamp NOT NULL DEFAULT now(),
//              last_modified_date timestamp NOT NULL DEFAULT now(),
//              created_by varchar(100),
//              last_modified_by varchar(100)
//            );
//            CREATE INDEX idx_repeat_instances_submission_path ON repeat_instance(submission_id, repeat_path);
//            """;
//        jdbc.execute(ddl);
//    }
//
//    @AfterAll
//    public void tearDown() {
//        if (ds instanceof HikariDataSource) {
//            ((HikariDataSource) ds).close();
//        }
//        PG.stop();
//    }
//
//    @Test
//    public void testSingleUpsertAndFindActive() {
//        RepeatInstance ri = RepeatInstance.builder().
//            id("01FZZZZZZZZZZZZZZZZZZZZZZ") // example ULID-like
//            .submission("sub1")
//            .repeatPath("people.adult")
//            .repeatIndex(0L)
//            .clientUpdatedAt(Instant.now())
//            .createdBy("tester")
//            .lastModifiedBy("tester").build();
//
//        dao.upsertRepeatInstance(ri);
//
//        // verify exists and active
//        List<String> uids = dao.findActiveRepeatUids("sub1", "people.adult");
//        assertEquals(1, uids.size());
//        assertEquals(ri.getId(), uids.get(0));
//    }
//
//    @Test
//    public void testBatchUpsertAndMarkDeleted() {
//        RepeatInstance r1 = RepeatInstance.builder()
//            .id("01F11111111111111111111111")
//            .submission("sub2")
//            .repeatPath("household.children")
//            .repeatIndex(0L)
//            .createdBy("tester").build();
//
//        RepeatInstance r2 = RepeatInstance.builder()
//            .id("01F22222222222222222222222")
//            .submission("sub2")
//            .repeatPath("household.children")
//            .repeatIndex(1L)
//            .createdBy("tester").build();
//
//        dao.upsertRepeatInstancesBatch(List.of(r1, r2));
//
//        List<String> existing = dao.findActiveRepeatUids("sub2", "household.children");
//        assertEquals(2, existing.size());
//
//        // mark r1 deleted
//        dao.markRepeatInstancesDeleted("sub2", "household.children", List.of(r1.getId()));
//
//        List<String> after = dao.findActiveRepeatUids("sub2", "household.children");
//        assertEquals(1, after.size());
//        assertEquals(r2.getId(), after.get(0));
//    }
//}
