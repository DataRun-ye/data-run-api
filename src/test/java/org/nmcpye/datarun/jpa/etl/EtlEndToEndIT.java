//package org.nmcpye.datarun.jpa.etl;
//
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//import lombok.Getter;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
//import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
//import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
//import org.nmcpye.datarun.jpa.etl.dao.RepeatInstancesJdbcDao;
//import org.nmcpye.datarun.jpa.etl.dao.SubmissionValuesJdbcDao;
//import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
//import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
//import org.nmcpye.datarun.jpa.etl.service.NormalizedSubmissionPersister;
//import org.nmcpye.datarun.jpa.etl.service.SubmissionNormalizer;
//import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//import javax.sql.DataSource;
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///**
// * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class EtlEndToEndIT {
//
//    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2")
//        .withDatabaseName("testdb")
//        .withUsername("test")
//        .withPassword("test");
//
//    private DataSource ds;
//    private JdbcTemplate jdbc;
//    private NamedParameterJdbcTemplate namedJdbc;
//
//    private RepeatInstancesJdbcDao repeatDao;
//    private SubmissionValuesJdbcDao valuesDao;
//    private NormalizedSubmissionPersister persister;
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
//
//        jdbc = new JdbcTemplate(ds);
//        namedJdbc = new NamedParameterJdbcTemplate(ds);
//
//        // Create tables used by DAOs (DDL must match your production schema)
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
//            CREATE INDEX idx_repeat_instance_submission_path ON repeat_instance(submission_id, repeat_path);
//
//            CREATE TABLE element_data_value (
//              id bigserial PRIMARY KEY,
//              submission_id varchar(50) NOT NULL,
//              repeat_instance_id varchar(26),
//              element_id varchar(200) NOT NULL,
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
//            CREATE UNIQUE INDEX ux_element_value_submission_element_repeat ON element_data_value (submission_id, element_id, repeat_instance_id);
//            CREATE INDEX idx_element_data_value_submission_repeat ON element_data_value(submission_id, repeat_instance_id);
//            """;
//        jdbc.execute(ddl);
//
//        // instantiate DAOs (NamedParameterJdbcTemplate-based implementations you kept)
//        repeatDao = new RepeatInstancesJdbcDao(namedJdbc);
//        valuesDao = new SubmissionValuesJdbcDao(namedJdbc);
//
//        // persister that uses those DAOs
//        persister = new NormalizedSubmissionPersister(repeatDao, valuesDao);
//    }
//
//    @AfterAll
//    public void cleanup() {
//        if (ds instanceof HikariDataSource) ((HikariDataSource) ds).close();
//        PG.stop();
//    }
//
//    @Test
//    public void testFullPipeline_insertAndMarkSweep() {
//        // Build template DTO describing a form:
//        // top-level field "household_head_name"
//        // repeat section "members" with a child field "age" and category element "role"
//
//        FormDataElementConf headName = new FormDataElementConf();
//        headName.setId("el_head_name");
//        headName.setName("household_head_name");
//        headName.setPath("household_head_name");
//
//        FormDataElementConf memberAge = new FormDataElementConf();
//        memberAge.setId("el_member_age");
//        memberAge.setName("age");
//        memberAge.setPath("members.age");
//
//        FormDataElementConf memberRole = new FormDataElementConf();
//        memberRole.setId("el_member_role");
//        memberRole.setName("role");
//        memberRole.setPath("members.role");
//
//        FormSectionConf membersSection = new FormSectionConf();
//        membersSection.setName("members");
//        membersSection.setPath("members");
//        membersSection.setRepeatable(Boolean.TRUE);
//        membersSection.setRepeatCategoryElement(memberRole.getId());
//
//        DataTemplateInstanceDto dto = new DataTemplateInstanceDto(
//            /*id*/ "tmpl1",
//            /*versionUid*/ "v1",
//            /*versionNumber*/ 1,
//            /*name*/ "household",
//            /*description*/ "desc",
//            /*deleted*/ false,
//            /*label*/ Map.of(),
//            /*fields*/ List.of(headName, memberAge, memberRole),
//            /*sections*/ List.of(membersSection)
//        );
//
//        TemplateElementMap elementMap = new TemplateElementMap(dto);
//
//        // Build a submission with 2 repeat items
//        Map<String, Object> formData = new HashMap<>();
//        formData.put("household_head_name", "Alice");
//
//        Map<String, Object> member1 = new HashMap<>();
//        member1.put("_uid", "UID-1");
//        member1.put("age", 34);
//        member1.put("role", "father");
//
//        Map<String, Object> member2 = new HashMap<>();
//        member2.put("_uid", "UID-2");
//        member2.put("age", 10);
//        member2.put("role", "child");
//
//        formData.put("members", List.of(member1, member2));
//
//        TestDataFormSubmission submission = new TestDataFormSubmission("sub-1", "tmpl1", "v1", "assign-1", formData, Instant.now());
//
//        // run normalization
//        SubmissionNormalizer normalizer = new SubmissionNormalizer(elementMap, /*generateMissingRepeatUids=*/ false);
//        NormalizedSubmission ns = normalizer.normalize(submission);
//
//        // persist
//        persister.persist(ns);
//
//        // assertions: repeat_instance has 2 active UIDs
//        List<String> dbUids = jdbc.queryForList(
//            "SELECT id FROM repeat_instance WHERE submission_id = ? AND repeat_path = ? AND deleted_at IS NULL",
//            String.class, submission.instanceId(), "members");
//        assertEquals(2, dbUids.size());
//        assertTrue(dbUids.contains("UID-1"));
//        assertTrue(dbUids.contains("UID-2"));
//
//        // element_data_value should contain head_name + two age rows + two role (category) rows = 5
//        Integer totalCount = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ?",
//            Integer.class, submission.instanceId());
//        assertEquals(5, totalCount.intValue(), "Expect 1 top-level + 2 ages + 2 category rows");
//
//        // verify role (category) rows exist (members.role saved twice)
//        Integer roleCount = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ?",
//            Integer.class, submission.instanceId(), "el_member_role");
//        assertEquals(2, roleCount.intValue());
//
//        // verify age rows exist and their values and category_id linkage
//        Integer ageCount = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ?",
//            Integer.class, submission.instanceId(), "el_member_age");
//        assertEquals(2, ageCount.intValue());
//
//        BigDecimal ageValUid1 = jdbc.queryForObject(
//            "SELECT value_num FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND element_id = ?",
//            BigDecimal.class, submission.instanceId(), "UID-1", "el_member_age");
//        assertEquals(BigDecimal.valueOf(34), ageValUid1);
//
//        // ensure the age row has the category_id set to the role value for UID-1
//        String catForUid1 = jdbc.queryForObject(
//            "SELECT category_id FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND element_id = ?",
//            String.class, submission.instanceId(), "UID-1", "el_member_age");
//        assertEquals("father", catForUid1);
//
//        // ensure stored role value equals the category value above
//        String storedRoleValue = jdbc.queryForObject(
//            "SELECT value_text FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND element_id = ?",
//            String.class, submission.instanceId(), "UID-1", "el_member_role");
//        assertEquals(catForUid1, storedRoleValue);
//
//        // Now simulate an updated submission where UID-1 is removed (only UID-2 remains).
//        Map<String, Object> formData2 = new HashMap<>();
//        formData2.put("household_head_name", "Alice");
//        formData2.put("members", List.of(member2));
//
//        TestDataFormSubmission submission2 = new TestDataFormSubmission("sub-1", "tmpl1", "v1", "assign-1", formData2, Instant.now());
//        NormalizedSubmission ns2 = normalizer.normalize(submission2);
//
//        // persist again -> mark-and-sweep should mark UID-1 deleted and its values deleted
//        persister.persist(ns2);
//
//        // UID-1 should be marked deleted (deleted_at NOT NULL), only one active left
//        Integer activeAfter = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM repeat_instance WHERE submission_id = ? AND repeat_path = ? AND deleted_at IS NULL",
//            Integer.class, submission.instanceId(), "members");
//        assertEquals(1, activeAfter.intValue());
//
//        // element_data_value for UID-1 should have deleted_at NOT NULL (so not counted as active)
//        Integer notDeletedValuesForUid1 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND deleted_at IS NULL",
//            Integer.class, submission.instanceId(), "UID-1");
//        assertEquals(0, notDeletedValuesForUid1.intValue());
//
//        // remaining values for UID-2 still present
//        Integer stillPresentUid2 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND repeat_instance_id = ? AND deleted_at IS NULL",
//            Integer.class, submission.instanceId(), "UID-2");
//        assertTrue(stillPresentUid2 > 0);
//    }
//
//    // lightweight test submission class used only inside these tests
//    @Getter
//    static class TestDataFormSubmission extends DataFormSubmission {
//        private final String instanceId;
//        private final String form;
//        private final String formVersion;
//        private final String assignment;
//        private final Map<String, Object> formData;
//        private final Instant finishedEntryTime;
//
//        TestDataFormSubmission(String instanceId, String form, String formVersion, String assignment, Map<String, Object> formData, Instant finishedEntryTime) {
//            this.instanceId = instanceId;
//            this.form = form;
//            this.formVersion = formVersion;
//            this.assignment = assignment;
//            this.formData = formData;
//            this.finishedEntryTime = finishedEntryTime;
//        }
//
//        public String instanceId() { return instanceId; }
//    }
//}
