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
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
//import org.nmcpye.datarun.jpa.etl.repository.RepeatInstancesJdbcDao;
//import org.nmcpye.datarun.jpa.etl.dao.SubmissionValuesJdbcDao;
//import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
//import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
//import org.nmcpye.datarun.jpa.etl.service.NormalizedSubmissionPersister;
//import org.nmcpye.datarun.jpa.etl.service.SubmissionNormalizer;
//import org.nmcpye.datarun.jpa.option.Option;
//import org.nmcpye.datarun.jpa.option.OptionGroup;
//import org.nmcpye.datarun.jpa.option.OptionGroupSet;
//import org.nmcpye.datarun.jpa.option.OptionSet;
//import org.nmcpye.datarun.jpa.option.exception.InvalidOptionCodesException;
//import org.nmcpye.datarun.jpa.option.service.OptionService;
//import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.time.Instant;
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.stream.Collectors;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Integration tests exercising the full ETL pipeline (multi-select add/remove/clear),
// * DAO null-repeat handling, and advisory lock concurrency behaviour.
// *
// * @author Assistant 14/08/2025
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class EtlFullPipelineIT {
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
//    private SubmissionValuesJdbcDao valuesDao;
//    private RepeatInstancesJdbcDao repeatDao; // small test-only impl below
//    private NormalizedSubmissionPersister persister;
//    private OptionService optionService; // test stub
//
//    @BeforeAll
//    public void setupAll() throws Exception {
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
//        // create schema used in tests (repeat_instance + element_data_value with partial unique indexes)
//        String ddl = """
//               CREATE TABLE repeat_instance (
//                 id varchar(26) PRIMARY KEY,
//                 submission_id varchar(50) NOT NULL,
//                 repeat_path varchar(3000) NOT NULL,
//                 repeat_index bigint,
//                 client_updated_at timestamp,
//                 deleted_at timestamp,
//                 created_date timestamp NOT NULL DEFAULT now(),
//                 last_modified_date timestamp NOT NULL DEFAULT now(),
//                 created_by varchar(100),
//                 last_modified_by varchar(100)
//            );
//            CREATE INDEX IF NOT EXISTS idx_repeat_instance_submission_path ON repeat_instance(submission_id, repeat_path);
//
//            CREATE TABLE element_data_value (
//               id bigserial PRIMARY KEY,
//               submission_id varchar(50) NOT NULL,
//               repeat_instance_id varchar(26),
//               element_id varchar(200) NOT NULL,
//               option_id varchar(100),
//               value_text text,
//               value_num numeric,
//               value_bool boolean,
//               assignment_id varchar(50),
//               template_id varchar(50) NOT NULL,
//               category_id varchar(200),
//               deleted_at timestamp,
//               created_date timestamp NOT NULL DEFAULT now(),
//               last_modified_date timestamp NOT NULL DEFAULT now(),
//               -- computed/stored columns for stable conflict target
//               repeat_instance_key text GENERATED ALWAYS AS (COALESCE(repeat_instance_id, '')) STORED,
//               selection_key text GENERATED ALWAYS AS (COALESCE(option_id, '')) STORED,
//               -- row_type: 'S' single-value, 'M' multi-select row
//               row_type char(1) NOT NULL DEFAULT 'S'
//            );
//
//            -- unified unique index encoding the semantics:
//            --  - single-value rows => row_type='S', selection_key = ''  => unique per (submission, element, repeat)
//            --  - multi-select rows => row_type='M', selection_key = option_id => unique per option_id
//            CREATE UNIQUE INDEX ux_element_value_unique
//              ON element_data_value (
//                submission_id,
//                element_id,
//                repeat_instance_key,
//                row_type,
//                selection_key
//              );
//
//            -- helper index for lookups
//            CREATE INDEX idx_element_data_value_submission_repeat ON element_data_value(submission_id, repeat_instance_id);
//            """;
//        jdbc.execute(ddl);
//
//        // instantiate DAOs and persister
//        valuesDao = new SubmissionValuesJdbcDao(namedJdbc);
//        repeatDao = new RepeatInstancesJdbcDao(namedJdbc);
//        persister = new NormalizedSubmissionPersister(repeatDao, valuesDao, jdbc);
//
//        // OptionService stub: accepts opt1,opt2,opt3 mapping to "id-opt1" etc.
//        optionService = new OptionService() {
//            @Override
//            public OptionSet saveOptionSet(OptionSet optionSet) {
//                return null;
//            }
//
//            @Override
//            public void updateOptionSet(OptionSet optionSet) {
//
//            }
//
//            @Override
//            public Optional<OptionSet> getOptionSet(String id) {
//                return Optional.empty();
//            }
//
//            @Override
//            public Optional<OptionSet> getOptionSetByName(String name) {
//                return Optional.empty();
//            }
//
//            @Override
//            public Optional<OptionSet> getOptionSetByCode(String code) {
//                return Optional.empty();
//            }
//
//            @Override
//            public void deleteOptionSet(OptionSet optionSet) {
//
//            }
//
//            @Override
//            public List<OptionSet> getAllOptionSets() {
//                return List.of();
//            }
//
//            @Override
//            public List<Option> getOptions(String optionSetId, String name, Integer max) {
//                return List.of();
//            }
//
//            @Override
//            public void updateOption(Option option) {
//
//            }
//
//            @Override
//            public Optional<Option> getOption(String id) {
//                return Optional.empty();
//            }
//
//            @Override
//            public Map<String, String> validateAndMapOptionCodes(Collection<String> optionCodes, String optionSetId) throws InvalidOptionCodesException {
//                if (optionCodes == null || optionCodes.isEmpty()) return Collections.emptyMap();
//                // create mapping if code starts with "opt", otherwise treat as missing
//                List<String> missing = optionCodes.stream()
//                    .filter(c -> c == null || !c.startsWith("opt"))
//                    .distinct()
//                    .collect(Collectors.toList());
//                if (!missing.isEmpty()) throw new InvalidOptionCodesException(missing);
//                return optionCodes.stream().distinct().collect(Collectors.toMap(c -> c, c -> "id-" + c));
//            }
//
//            @Override
//            public Optional<Option> getOptionByCode(String code) {
//                return Optional.empty();
//            }
//
//            @Override
//            public List<Option> getOptionsByCode(List<String> code) {
//                return List.of();
//            }
//
//            @Override
//            public void deleteOption(Option option) {
//
//            }
//
//            @Override
//            public OptionGroup saveOptionGroup(OptionGroup group) {
//                return null;
//            }
//
//            @Override
//            public void updateOptionGroup(OptionGroup group) {
//
//            }
//
//            @Override
//            public Optional<OptionGroup> getOptionGroup(String id) {
//                return Optional.empty();
//            }
//
//            @Override
//            public void deleteOptionGroup(OptionGroup group) {
//
//            }
//
//            @Override
//            public List<OptionGroup> getAllOptionGroups() {
//                return List.of();
//            }
//
//            @Override
//            public OptionGroupSet saveOptionGroupSet(OptionGroupSet group) {
//                return null;
//            }
//
//            @Override
//            public void updateOptionGroupSet(OptionGroupSet group) {
//
//            }
//
//            @Override
//            public Optional<OptionGroupSet> getOptionGroupSet(String id) {
//                return Optional.empty();
//            }
//
//            @Override
//            public void deleteOptionGroupSet(OptionGroupSet group) {
//
//            }
//
//            @Override
//            public List<OptionGroupSet> getAllOptionGroupSets() {
//                return List.of();
//            }
//        };
//    }
//
//    @AfterAll
//    public void tearDownAll() {
//        if (ds instanceof HikariDataSource) ((HikariDataSource) ds).close();
//        PG.stop();
//    }
//
//    // ------------ Test: multi-select add/remove/clear ------------
//    @Test
//    public void multiSelect_add_remove_clear_flow() {
//        // Build a simple template with a multi-select top-level field "hobbies"
//        FormDataElementConf hobby = new FormDataElementConf();
//        hobby.setId("el_hobby");
//        hobby.setName("hobbies");
//        hobby.setPath("hobbies");
//        hobby.setType(ValueType.SelectMulti);
//        hobby.setOptionSet("os1");
//
//        DataTemplateInstanceDto dto =
//            new DataTemplateInstanceDto("tmpl1", "v1", 1, "tm",
//                "d", false, Map.of(), List.of(hobby), List.of());
//        TemplateElementMap elementMap = new TemplateElementMap(dto);
//
//        // Normalizer wired with optionService stub
//        SubmissionNormalizer normalizer = new SubmissionNormalizer(optionService, elementMap,
//            true);
//
//        // Submission 1: select opt1 and opt2
//        Map<String, Object> form1 = new HashMap<>();
//        form1.put("hobbies", List.of("opt1", "opt2"));
//        TestDataFormSubmission submission1 = new TestDataFormSubmission("sub-1", "tmpl1", "v1", "assign-1", form1, Instant.now());
//
//        NormalizedSubmission ns1 = normalizer.normalize(submission1);
//        persister.persist(ns1);
//
//        // After persist: two multi-select rows should exist with option_id id-opt1 and id-opt2
//        List<String> ids1 = jdbc.queryForList("SELECT option_id FROM element_data_value WHERE submission_id = ? AND element_id = ? AND deleted_at IS NULL",
//            String.class, submission1.instanceId(), "el_hobby");
//        assertEquals(2, ids1.size());
//        assertTrue(ids1.contains("id-opt1"));
//        assertTrue(ids1.contains("id-opt2"));
//
//        // Submission 2: remove opt1 -> only opt2 remains
//        Map<String, Object> form2 = new HashMap<>();
//        form2.put("hobbies", List.of("opt2"));
//        TestDataFormSubmission submission2 = new TestDataFormSubmission("sub-1", "tmpl1", "v1", "assign-1", form2, Instant.now());
//        NormalizedSubmission ns2 = normalizer.normalize(submission2);
//        persister.persist(ns2);
//
//        // opt1 should be marked deleted, opt2 still active
//        Integer activeCountOpt1 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND option_id = ? AND deleted_at IS NULL",
//            Integer.class, submission1.instanceId(), "el_hobby", "id-opt1");
//        assertEquals(0, activeCountOpt1.intValue());
//
//        Integer activeCountOpt2 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND option_id = ? AND deleted_at IS NULL",
//            Integer.class, submission1.instanceId(), "el_hobby", "id-opt2");
//        assertEquals(1, activeCountOpt2.intValue());
//
//        // Submission 3: explicit empty list -> clear remaining selection(s)
//        Map<String, Object> form3 = new HashMap<>();
//        form3.put("hobbies", List.of()); // explicit clear
//        TestDataFormSubmission submission3 = new TestDataFormSubmission("sub-1", "tmpl1", "v1", "assign-1", form3, Instant.now());
//        NormalizedSubmission ns3 = normalizer.normalize(submission3);
//        persister.persist(ns3);
//
//        // now opt2 should be marked deleted too
//        Integer activeAfterClear = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND deleted_at IS NULL",
//            Integer.class, submission1.instanceId(), "el_hobby");
//        assertEquals(0, activeAfterClear.intValue());
//    }
//
//    // ------------ Test: DAO null repeat parameter handling ------------
//    @Test
//    public void dao_null_repeat_parameter_handling() {
//        String submission = "s-null";
//        String element = "el_tag";
//        String repeat = null;
//
//        // insert a legacy row with option_id = null and value_text set
//        jdbc.update("INSERT INTO element_data_value (submission_id, repeat_instance_id, element_id, value_text, template_id) VALUES (?, ?, ?, ?, ?)",
//            submission, null, element, "legacy", "tmpl");
//
//        // findSelectionIdentitiesForElementRepeat should return the value_text when repeatInstanceId == null
//        List<String> ids = valuesDao.findSelectionIdentitiesForElementRepeat(submission, repeat, element);
//        assertEquals(1, ids.size());
//        assertEquals("legacy", ids.get(0));
//
//        // markSelectionValuesDeletedByIdentity should not throw and should mark row deleted
//        valuesDao.markSelectionValuesDeletedByIdentity(submission, repeat, element, List.of("legacy"));
//        Integer deleted = jdbc.queryForObject("SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND deleted_at IS NOT NULL", Integer.class,
//            submission, element);
//        assertEquals(1, deleted.intValue());
//    }
//
//    // ------------ Test: advisory lock serializes concurrent persists ------------
//    @Test
//    public void advisoryLock_serializes_concurrent_persists() throws Exception {
//        // Build a minimal template with a single top-level field so normalizer produces at least one row
//        FormDataElementConf f = new FormDataElementConf();
//        f.setId("el_name");
//        f.setName("name");
//        f.setPath("name");
//        f.setType(ValueType.Text);
//
//        DataTemplateInstanceDto dto = new DataTemplateInstanceDto("tmpl-lock", "v1", 1, "t", "d", false, Map.of(), List.of(f), List.of());
//        TemplateElementMap map = new TemplateElementMap(dto);
//        SubmissionNormalizer normalizer = new SubmissionNormalizer(optionService, map, true);
//
//        Map<String, Object> form = new HashMap<>();
//        form.put("name", "Alice");
//        TestDataFormSubmission submission = new TestDataFormSubmission("sub-lock", "tmpl-lock", "v1", "assign", form, Instant.now());
//
//        NormalizedSubmission ns = normalizer.normalize(submission);
//
//        // compute the advisory lock key identical to persister's algorithm
//        long lockKey = computeAdvisoryLockKey(submission.instanceId());
//
//        // Start a background holder that acquires pg_advisory_xact_lock using a manual Connection and holds it until released.
//        CountDownLatch holderAcquired = new CountDownLatch(1);
//        CountDownLatch holderRelease = new CountDownLatch(1);
//
//        ExecutorService exec = Executors.newSingleThreadExecutor();
//        Future<?> holderFuture = exec.submit(() -> {
//            try (Connection conn = ds.getConnection()) {
//                conn.setAutoCommit(false);
//                try (PreparedStatement ps = conn.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
//                    ps.setLong(1, lockKey);
//                    ps.execute();
//                    // now the lock is acquired and held until this transaction ends (conn.close() / commit)
//                    holderAcquired.countDown();
//                    // wait until test signals to release
//                    holderRelease.await(10, TimeUnit.SECONDS);
//                    conn.commit();
//                }
//            } catch (Throwable t) {
//                throw new RuntimeException(t);
//            }
//        });
//
//        // Wait until holder acquired lock
//        assertTrue(holderAcquired.await(5, TimeUnit.SECONDS));
//
//        // Now start persister.persist in another thread — it should block when trying to acquire the same advisory lock.
//        ExecutorService workerExec = Executors.newSingleThreadExecutor();
//        Future<?> workerFuture = workerExec.submit(() -> {
//            // This call will block inside persister until holder releases the lock
//            persister.persist(ns);
//            return null;
//        });
//
//        // Give a short moment to ensure worker attempted to acquire and is blocked (can't easily detect blocked state, but we can assert worker not done yet)
//        Thread.sleep(500);
//
//        assertFalse(workerFuture.isDone(), "Worker should be blocked waiting for advisory lock");
//
//        // Release holder
//        holderRelease.countDown();
//
//        // worker should finish now
//        workerFuture.get(5, TimeUnit.SECONDS);
//
//        // cleanup
//        exec.shutdownNow();
//        workerExec.shutdownNow();
//    }
//
//    // ----------------- small helpers & test-only RepeatInstances JDBC impl -----------------
//    @Getter
//    static class TestDataFormSubmission extends DataFormSubmission {
//        private final String form;
//        private final String formVersion;
//        private final String assignment;
//        private final Map<String, Object> formData;
//        private final Instant finishedEntryTime;
//
//        TestDataFormSubmission(String instanceId, String form, String formVersion,
//                               String assignment, Map<String, Object> formData,
//                               Instant finishedEntryTime) {
//            this.setUid(instanceId);
//            this.form = form;
//            this.formVersion = formVersion;
//            this.assignment = assignment;
//            this.formData = formData;
//            this.finishedEntryTime = finishedEntryTime;
//        }
//
//        public String instanceId() {
//            return getUid();
//        }
//    }
//
//    private long computeAdvisoryLockKey(String submissionId) {
//        // same FNV-1a 64-bit as used in persister implementation earlier
//        long h = 1469598103934665603L;
//        for (char c : submissionId.toCharArray()) {
//            h ^= c;
//            h *= 1099511628211L;
//        }
//        return h;
//    }
//}
