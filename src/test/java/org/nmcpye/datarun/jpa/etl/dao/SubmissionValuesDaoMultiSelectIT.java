//package org.nmcpye.datarun.jpa.etl.dao;
//
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//import javax.sql.DataSource;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///**
// * @author Hamza Assada 14/08/2025 (7amza.it@gmail.com)
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class SubmissionValuesDaoMultiSelectIT {
//
//    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2")
//        .withDatabaseName("testdb")
//        .withUsername("test")
//        .withPassword("test");
//
//    private DataSource ds;
//    private JdbcTemplate jdbc;
//    private NamedParameterJdbcTemplate namedJdbc;
//    private SubmissionValuesJdbcDao dao;
//
//    @BeforeAll
//    public void beforeAll() {
//        PG.start();
//        HikariConfig cfg = new HikariConfig();
//        cfg.setJdbcUrl(PG.getJdbcUrl());
//        cfg.setUsername(PG.getUsername());
//        cfg.setPassword(PG.getPassword());
//        ds = new HikariDataSource(cfg);
//        jdbc = new JdbcTemplate(ds);
//        namedJdbc = new NamedParameterJdbcTemplate(ds);
//
//        // create table (simple schema matching DAO expectations)
//        String ddl = """
//            CREATE TABLE element_data_value (
//              id bigserial PRIMARY KEY,
//              submission_id varchar(50) NOT NULL,
//              repeat_instance_id varchar(26),
//              element_id varchar(200) NOT NULL,
//              option_id varchar(100),
//              value_text text,
//              value_num numeric,
//              value_bool boolean,
//              assignment_id varchar(50),
//              template_id varchar(50) NOT NULL,
//              category_id varchar(200),
//              deleted_at timestamp,
//              created_date timestamp NOT NULL DEFAULT now(),
//              last_modified_date timestamp NOT NULL DEFAULT now()
//            );
//            CREATE UNIQUE INDEX ux_element_single_value ON element_data_value (submission_id, element_id, COALESCE(repeat_instance_id, '')) WHERE option_id IS NULL;
//            CREATE UNIQUE INDEX ux_element_multi_value ON element_data_value (submission_id, element_id, COALESCE(repeat_instance_id, ''), option_id) WHERE option_id IS NOT NULL;
//            """;
//        jdbc.execute(ddl);
//
//        dao = new SubmissionValuesJdbcDao(namedJdbc);
//    }
//
//    @AfterAll
//    public void afterAll() {
//        if (ds instanceof HikariDataSource) ((HikariDataSource) ds).close();
//        PG.stop();
//    }
//
//    @Test
//    public void testFindAndMarkSelectionIdentities() {
//        String submission = "s-1";
//        String element = "el_hobby";
//        String repeat = "RID-1";
//
//        // insert two multi-select rows (option_id = opt1,opt2)
//        ElementDataValue r1 = ElementDataValue.builder()
//            .submissionId(submission)
//            .repeatInstanceId(repeat)
//            .elementId(element)
//            .optionId("opt1")
//            .templateId("tmpl")
//            .assignmentId("a1").build();
//
//        ElementDataValue r2 = ElementDataValue.builder()
//            .submissionId(submission)
//            .repeatInstanceId(repeat)
//            .elementId(element)
//            .optionId("opt2")
//            .templateId("tmpl")
//            .assignmentId("a1").build();
//
//        dao.upsertSubmissionValuesBatch(List.of(r1, r2));
//
//        // verify identities returned
//        List<String> ids = dao.findSelectionIdentitiesForElementRepeat(submission, repeat, element);
//        assertTrue(ids.contains("opt1") && ids.contains("opt2") && ids.size() == 2);
//
//        // mark opt1 deleted
//        dao.markSelectionValuesDeletedByIdentity(submission, repeat, element, List.of("opt1"));
//
//        // verify opt1 row is marked deleted, opt2 stays active
//        Integer activeCount = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND element_id = ? AND deleted_at IS NULL",
//            Integer.class, submission, repeat, element);
//        assertEquals(1, activeCount.intValue());
//
//        String valTextOpt1 = jdbc.queryForObject(
//            "SELECT value_text FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND element_id = ? AND option_id = ?",
//            String.class, submission, repeat, element, "opt1");
//        // the row exists but should be marked deleted
//        Integer deletedCountOpt1 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND element_id = ? AND option_id = ? AND deleted_at IS NOT NULL",
//            Integer.class, submission, repeat, element, "opt1");
//        assertEquals(1, deletedCountOpt1.intValue());
//        assertEquals("reading", valTextOpt1);
//
//        // opt2 still active
//        Integer deletedCountOpt2 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND element_id = ? AND option_id = ? AND deleted_at IS NULL",
//            Integer.class, submission, repeat, element, "opt2");
//        assertEquals(1, deletedCountOpt2.intValue());
//    }
//
//    @Test
//    public void testMarkSelectionByValueTextFallback() {
//        // scenario: option_id is null (legacy label-only row)
//        String submission = "s-2";
//        String element = "el_tag";
//        String repeat = null; // top-level
//
//        // insert two multi-select rows (option_id = opt1,opt2)
//        ElementDataValue r = ElementDataValue.builder()
//            .submissionId(submission)
//            .repeatInstanceId(null)
//            .elementId(element)
//            .optionId(null)
//            .templateId("tmpl")
//            .assignmentId("a1").build();
//
//        dao.upsertSubmissionValuesBatch(List.of(r));
//
//        // find identities (should return the value_text)
//        List<String> ids = dao.findSelectionIdentitiesForElementRepeat(submission, null, element);
//        assertEquals(1, ids.size());
//        assertEquals("legacy-label", ids.get(0));
//
//        // mark deleted by identity (value_text)
//        dao.markSelectionValuesDeletedByIdentity(submission, null, element, List.of("legacy-label"));
//
//        Integer deletedCount = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND deleted_at IS NOT NULL",
//            Integer.class, submission, element);
//        assertEquals(1, deletedCount.intValue());
//    }
//}
