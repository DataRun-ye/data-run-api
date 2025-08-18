//package org.nmcpye.datarun.jpa.etl;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
//import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.etl.dao.RepeatInstancesJdbcDao;
//import org.nmcpye.datarun.jpa.etl.dao.SubmissionValuesJdbcDao;
//import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
//import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
//import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
//import org.nmcpye.datarun.jpa.etl.service.NormalizedSubmissionPersister;
//import org.nmcpye.datarun.jpa.etl.service.SubmissionNormalizer;
//import org.nmcpye.datarun.jpa.etl.stubs.InMemoryOptionServiceStub;
//import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Integration tests exercising a few ETL scenarios using Testcontainers.
// * <p>
// * Depends on AbstractEtlIntegrationTest which creates the DB and provides jdbc/namedJdbc.
// *
// * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class EtlIntegrationScenariosIT extends AbstractEtlIntegrationTest {
//
//    private SubmissionValuesJdbcDao valuesDao;
//    private RepeatInstancesJdbcDao repeatDao;
//    private NormalizedSubmissionPersister persister;
//    private InMemoryOptionServiceStub optionService;
//    private ObjectMapper objectMapper = new ObjectMapper();
//
//    @BeforeEach
//    public void beforeEachTest() {
//        truncateTables();
//        // (re)create DAOs/persister/stub for each test to avoid state leaks
//        valuesDao = new SubmissionValuesJdbcDao(namedJdbc);
//        repeatDao = new RepeatInstancesJdbcDao(namedJdbc);
//        persister = new NormalizedSubmissionPersister(repeatDao, valuesDao, jdbc);
//        optionService = new InMemoryOptionServiceStub();
//    }
//
//    // small inner test submission class (doesn't rely on Spring)
//    static public class TestDataFormSubmission extends DataFormSubmission {
//        TestDataFormSubmission(String instanceId, String form, String formVersion, String assignment, Map<String, Object> formData, Instant finishedEntryTime) {
//            this.setUid(instanceId); // DataFormSubmission#setUid exists (as in your test)
//            this.setForm(form);
//            this.setFormVersion(formVersion);
//            this.setAssignment(assignment);
//            this.setFormData(formData);
//            this.setFinishedEntryTime(finishedEntryTime);
//        }
//    }
//
//    @Test
//    public void nestedRepeats_flow() {
//        // Template: repeat section "people" with two fields: name, age
//        FormSectionConf people = TestBuilders.repeatSection("people", "people", true, null);
//        FormDataElementConf name = new FormDataElementConf();
//        name.setId("el_name");
//        name.setName("name");
//        name.setPath("people.name");
//        name.setType(ValueType.Text);
//
//        FormDataElementConf age = new FormDataElementConf();
//        age.setId("el_age");
//        age.setName("age");
//        age.setPath("people.age");
//        age.setType(ValueType.Integer);
//
//        TemplateElementMap map = TestBuilders.template("tmpl-nested", List.of(name, age), List.of(people));
//        SubmissionNormalizer normalizer = new SubmissionNormalizer(optionService, map, true);
//
//        Map<String, Object> item1 = new HashMap<>();
//        item1.put("_uid", "r1");
//        item1.put("name", "Alice");
//        item1.put("age", 30);
//
//        Map<String, Object> item2 = new HashMap<>();
//        item2.put("_uid", "r2");
//        item2.put("name", "Bob");
//        item2.put("age", 40);
//
//        Map<String, Object> form = new HashMap<>();
//        form.put("people", List.of(item1, item2));
//
//        TestDataFormSubmission submission = new TestDataFormSubmission("sub-nested", "tmpl-nested", "v1", "assign", form, Instant.now());
//
//        NormalizedSubmission ns = normalizer.normalize(submission);
//        // quick assertions on normalized object
//        assertEquals(2, ns.getRepeatInstances().size(), "two repeat instances expected");
//        assertTrue(ns.getValueRows().stream().anyMatch(r -> "el_name".equals(r.getElement()) && "Alice".equals(r.getValueText())));
//        assertTrue(ns.getValueRows().stream().anyMatch(r -> "el_age".equals(r.getElement()) && r.getValueNum() != null));
//
//        // persist and assert DB rows
//        persister.persist(ns);
//
//        Integer repeats = jdbc.queryForObject("SELECT COUNT(*) FROM repeat_instance WHERE submission_id = ?", Integer.class, submission.instanceId());
//        assertEquals(2, repeats.intValue());
//
//        Integer values = jdbc.queryForObject("SELECT COUNT(*) FROM element_data_value WHERE submission_id = ?", Integer.class, submission.instanceId());
//        assertEquals(ns.getValueRows().size(), values.intValue());
//    }
//
//    @Test
//    public void missingUid_generatesServerUid() {
//        // Template: simple repeat section "pets" with a single field "kind"
//        FormSectionConf pets = TestBuilders.repeatSection("pets", "pets", true, null);
//        FormDataElementConf kind = new FormDataElementConf();
//        kind.setId("el_kind");
//        kind.setName("kind");
//        kind.setPath("pets.kind");
//        kind.setType(ValueType.Text);
//
//        TemplateElementMap map = TestBuilders.template("tmpl-pets", List.of(kind), List.of(pets));
//        SubmissionNormalizer normalizer = new SubmissionNormalizer(optionService, map, true);
//
//        Map<String, Object> item = new HashMap<>();
//        item.put("kind", "dog"); // no _uid provided — normalizer should generate
//
//        Map<String, Object> form = new HashMap<>();
//        form.put("pets", List.of(item));
//
//        TestDataFormSubmission submission = new TestDataFormSubmission("sub-missing-uid", "tmpl-pets", "v1", "assign", form, Instant.now());
//
//        NormalizedSubmission ns = normalizer.normalize(submission);
//
//        // should have exactly one repeat instance and it should have a non-null id
//        assertEquals(1, ns.getRepeatInstances().size());
//        RepeatInstance ri = ns.getRepeatInstances().get(0);
//        assertNotNull(ri.getId());
//        assertFalse(ri.getId().isBlank());
//        assertEquals(submission.instanceId(), ri.getSubmission());
//
//        // persist and assert DB has the repeat instance and value row
//        persister.persist(ns);
//
//        Integer repeats = jdbc.queryForObject("SELECT COUNT(*) FROM repeat_instance WHERE submission_id = ?", Integer.class, submission.instanceId());
//        assertEquals(1, repeats.intValue());
//
//        Integer values = jdbc.queryForObject("SELECT COUNT(*) FROM element_data_value WHERE submission_id = ?", Integer.class, submission.instanceId());
//        assertEquals(ns.getValueRows().size(), values.intValue());
//    }
//
//    @Test
//    public void multiSelect_rowsCreatedAndSwept() {
//        // seed options in stub
//        optionService.addOption("opt1", "id-opt1");
//        optionService.addOption("opt2", "id-opt2");
//        optionService.addOption("opt3", "id-opt3");
//
//        // Template: top-level multi-select field "hobbies"
//        FormDataElementConf hobby = new FormDataElementConf();
//        hobby.setId("el_hobby");
//        hobby.setName("hobbies");
//        hobby.setPath("hobbies");
//        hobby.setType(ValueType.SelectMulti);
//        hobby.setOptionSet("os1");
//
//        TemplateElementMap map = TestBuilders.template("tmpl-multi", List.of(hobby), List.of());
//        SubmissionNormalizer normalizer = new SubmissionNormalizer(optionService, map, true);
//
//        // Submission 1: opt1 + opt2
//        Map<String, Object> form1 = new HashMap<>();
//        form1.put("hobbies", List.of("opt1", "opt2"));
//        TestDataFormSubmission s1 = new TestDataFormSubmission("sub-multi", "tmpl-multi", "v1", "assign", form1, Instant.now());
//        NormalizedSubmission ns1 = normalizer.normalize(s1);
//        persister.persist(ns1);
//
//        List<String> ids1 = DbTestHelpers.fetchOptionIdsFor(jdbc, s1.instanceId(), "el_hobby");
//        assertEquals(2, ids1.size());
//        assertTrue(ids1.contains("id-opt1"));
//        assertTrue(ids1.contains("id-opt2"));
//
//        // Submission 2: change to only opt2 (remove opt1)
//        Map<String, Object> form2 = new HashMap<>();
//        form2.put("hobbies", List.of("opt2"));
//        TestDataFormSubmission s2 = new TestDataFormSubmission("sub-multi", "tmpl-multi", "v1", "assign", form2, Instant.now());
//        NormalizedSubmission ns2 = normalizer.normalize(s2);
//        persister.persist(ns2);
//
//        Integer activeOpt1 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND option_id = ? AND deleted_at IS NULL",
//            Integer.class, s1.instanceId(), "el_hobby", "id-opt1");
//        assertEquals(0, activeOpt1.intValue());
//
//        Integer activeOpt2 = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND option_id = ? AND deleted_at IS NULL",
//            Integer.class, s1.instanceId(), "el_hobby", "id-opt2");
//        assertEquals(1, activeOpt2.intValue());
//
//        // Submission 3: explicit empty list -> clears remaining selection(s)
//        Map<String, Object> form3 = new HashMap<>();
//        form3.put("hobbies", List.of());
//        TestDataFormSubmission s3 = new TestDataFormSubmission("sub-multi", "tmpl-multi", "v1", "assign", form3, Instant.now());
//        NormalizedSubmission ns3 = normalizer.normalize(s3);
//        persister.persist(ns3);
//
//        Integer activeAfterClear = jdbc.queryForObject(
//            "SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND deleted_at IS NULL",
//            Integer.class, s1.instanceId(), "el_hobby");
//        assertEquals(0, activeAfterClear.intValue());
//    }
//}
