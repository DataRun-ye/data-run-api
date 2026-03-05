package org.nmcpye.datarun.service.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SubmissionDenormalizer}.
 * No Spring context needed — pure static functions.
 */
class SubmissionDenormalizerTest {

    private ObjectMapper mapper;
    private List<FormSectionConf> sections;
    private List<FormDataElementConf> fields;
    private static final String SUBMISSION_UID = "z3Ye07TDj7a";

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();

        // Sections
        FormSectionConf main = new FormSectionConf();
        main.setName("main");
        main.setRepeatable(false);
        main.setOrder(100);

        FormSectionConf patients = new FormSectionConf();
        patients.setName("patients");
        patients.setRepeatable(false);
        patients.setOrder(200);

        FormSectionConf medicines = new FormSectionConf();
        medicines.setName("medicines");
        medicines.setRepeatable(true);
        medicines.setOrder(300);

        FormSectionConf referrals = new FormSectionConf();
        referrals.setName("referrals");
        referrals.setRepeatable(false);
        referrals.setOrder(400);

        sections = List.of(main, patients, medicines, referrals);

        // Fields — minimal set needed for denormalization
        fields = List.of(
                buildField("visitdate", "main"),
                buildField("NotificationNumber", "main"),
                buildField("emergency_team_type", "main"),
                buildField("age", "patients"),
                buildField("gender", "patients"),
                buildField("PatientName", "patients"),
                buildField("ispregnant", "patients"),
                buildField("serialNumber", "patients"),
                buildField("diagnosed_disease_type", "patients"),
                buildField("symptoms", "patients"),
                buildField("lab_test", "patients"),
                buildField("cm_measures", "referrals"));
    }

    private FormDataElementConf buildField(String name, String parent) {
        FormDataElementConf field = new FormDataElementConf();
        field.setName(name);
        field.setParent(parent);
        return field;
    }

    @Test
    @DisplayName("Simple canonical with only values denormalizes into section wrappers")
    void simpleValuesToSections() throws Exception {
        String canonicalJson = """
                {
                  "values": {
                    "visitdate": "2025-09-27",
                    "NotificationNumber": 2,
                    "gender": "MALE",
                    "PatientName": "John"
                  },
                  "collections": {}
                }
                """;
        JsonNode canonical = mapper.readTree(canonicalJson);

        JsonNode result = SubmissionDenormalizer.denormalize(
                canonical, sections, fields, SUBMISSION_UID, mapper);

        // main section wrapper
        assertTrue(result.has("main"));
        assertEquals("2025-09-27", result.get("main").get("visitdate").asText());
        assertEquals(2, result.get("main").get("NotificationNumber").asInt());

        // patients section wrapper
        assertTrue(result.has("patients"));
        assertEquals("MALE", result.get("patients").get("gender").asText());
        assertEquals("John", result.get("patients").get("PatientName").asText());

        // medicines should be empty array (repeater section exists but no collections)
        assertTrue(result.has("medicines"));
        assertTrue(result.get("medicines").isArray());
        assertEquals(0, result.get("medicines").size());

        // referrals should be empty object (no values for that section)
        assertTrue(result.has("referrals"));
        assertTrue(result.get("referrals").isObject());
    }

    @Test
    @DisplayName("Collection map denormalizes back to V1 array with metadata restored")
    void collectionToArray() throws Exception {
        String canonicalJson = """
                {
                  "values": { "visitdate": "2025-09-27" },
                  "collections": {
                    "medicines": {
                      "01K693VTPPWQR1M23AN06B6N0D": {
                        "_index": 1,
                        "_parent_id": "z3Ye07TDj7a",
                        "amd": "act40_tape",
                        "druguom": "tablet",
                        "prescribed_quantity": 1
                      }
                    }
                  }
                }
                """;
        JsonNode canonical = mapper.readTree(canonicalJson);

        JsonNode result = SubmissionDenormalizer.denormalize(
                canonical, sections, fields, SUBMISSION_UID, mapper);

        // medicines should be a V1 array
        assertTrue(result.get("medicines").isArray());
        assertEquals(1, result.get("medicines").size());

        JsonNode row = result.get("medicines").get(0);
        assertEquals("01K693VTPPWQR1M23AN06B6N0D", row.get("_id").asText());
        assertEquals("act40_tape", row.get("amd").asText());
        assertEquals("tablet", row.get("druguom").asText());
        assertEquals(1, row.get("prescribed_quantity").asInt());
        assertEquals(1, row.get("_index").asInt());

        // V1 metadata restored
        assertEquals(SUBMISSION_UID, row.get("_parentId").asText());
        assertEquals(SUBMISSION_UID, row.get("_submissionUid").asText());
    }

    @Test
    @DisplayName("Nested repeater: _parent_id pointing to parent row is preserved in V1 as _parentId")
    void nestedRepeaterParentId() throws Exception {
        FormSectionConf households = new FormSectionConf();
        households.setName("households");
        households.setRepeatable(true);
        households.setOrder(100);

        FormSectionConf members = new FormSectionConf();
        members.setName("family_members");
        members.setRepeatable(true);
        members.setOrder(200);

        List<FormSectionConf> nestedSections = List.of(households, members);

        String canonicalJson = """
                {
                  "values": {},
                  "collections": {
                    "households": {
                      "hh_001": { "address": "123 Main St", "_parent_id": "sub_uid" }
                    },
                    "family_members": {
                      "mem_001": { "_parent_id": "hh_001", "name": "John", "age": 45 }
                    }
                  }
                }
                """;
        JsonNode canonical = mapper.readTree(canonicalJson);

        JsonNode result = SubmissionDenormalizer.denormalize(
                canonical, nestedSections, List.of(), "sub_uid", mapper);

        // family_members row should have _parentId = "hh_001" (parent row id)
        JsonNode memberRow = result.get("family_members").get(0);
        assertEquals("hh_001", memberRow.get("_parentId").asText());
        assertEquals("sub_uid", memberRow.get("_submissionUid").asText());
    }

    @Test
    @DisplayName("Null canonical produces empty V1 structure")
    void nullCanonical() {
        JsonNode result = SubmissionDenormalizer.denormalize(
                null, sections, fields, SUBMISSION_UID, mapper);

        assertNotNull(result);
        assertTrue(result.isObject());
    }

    @Test
    @DisplayName("Multi-select array values preserved during denormalization")
    void multiSelectArraysPreserved() throws Exception {
        String canonicalJson = """
                {
                  "values": {
                    "symptoms": ["fever", "headache"],
                    "gender": "MALE"
                  },
                  "collections": {}
                }
                """;
        JsonNode canonical = mapper.readTree(canonicalJson);

        JsonNode result = SubmissionDenormalizer.denormalize(
                canonical, sections, fields, SUBMISSION_UID, mapper);

        // symptoms should be an array inside the patients section wrapper
        JsonNode symptoms = result.get("patients").get("symptoms");
        assertNotNull(symptoms);
        assertTrue(symptoms.isArray());
        assertEquals(2, symptoms.size());
        assertEquals("fever", symptoms.get(0).asText());
    }
}
