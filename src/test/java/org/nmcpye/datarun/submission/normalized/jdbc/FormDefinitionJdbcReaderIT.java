package org.nmcpye.datarun.submission.normalized.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.security.jwt.JwtAuthenticationTestUtils;
import org.nmcpye.datarun.submission.normalized.FieldDefinition;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;
import org.nmcpye.datarun.submission.normalized.NormalizedRow;
import org.nmcpye.datarun.submission.normalized.NormalizedSubmission;
import org.nmcpye.datarun.submission.normalized.RowKeyKind;
import org.nmcpye.datarun.submission.normalized.SectionDefinition;
import org.nmcpye.datarun.submission.normalized.StoredSubmission;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Forms published through the real template route, then read back for the normalizer: the definitions as the server
 * stores them today, on the real schema. Ids and values are invented.
 */
@IntegrationTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormDefinitionJdbcReaderIT {

    private static final String FORM_UID = "s1FormNorm1";
    private static final Map<String, String> DATA_ELEMENTS = Map.of(
        "s1DeTitle01", "01JS1DE0000000000000000001",
        "s1DeCount01", "01JS1DE0000000000000000002",
        "s1DePart001", "01JS1DE0000000000000000003",
        "s1DeNote001", "01JS1DE0000000000000000004");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FormDefinitionJdbcReader reader;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void publishTwoVersions() throws Exception {
        Integer present = jdbc.queryForObject("SELECT count(*) FROM data_template WHERE uid = :uid",
            Map.of("uid", FORM_UID), Integer.class);
        if (present != null && present > 0) {
            return;
        }
        // the application's connections do not auto-commit
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            dataElement("s1DeTitle01", "s1_title", "Text");
            dataElement("s1DeCount01", "s1_count", "Integer");
            dataElement("s1DePart001", "s1_part", "Text");
            dataElement("s1DeNote001", "s1_note", "LongText");
        });
        publish("""
            {"uid": "s1FormNorm1", "name": "s1 normalized form",
             "fields": [
               {"id": "s1DeTitle01", "parent": "main", "type": "Text"},
               {"id": "s1DeNote001", "parent": "main", "type": "LongText"},
               {"id": "s1DeCount01", "parent": "rows", "type": "Integer"}],
             "sections": [
               {"name": "main"},
               {"name": "rows", "parent": "main", "repeatable": true}]}""");
        publish("""
            {"uid": "s1FormNorm1", "name": "s1 normalized form",
             "fields": [
               {"id": "s1DeTitle01", "type": "Text"},
               {"id": "s1DeCount01", "parent": "rows", "type": "Integer"},
               {"id": "s1DePart001", "parent": "parts", "type": "Text"}],
             "sections": [
               {"name": "main"},
               {"name": "rows", "parent": "main", "repeatable": true},
               {"name": "parts", "parent": "rows", "repeatable": true}]}""");
    }

    @Test
    void everyVersionIsReadWithDataPathsAndDataElementIds() {
        FormDefinition form = reader.read(FORM_UID).orElseThrow();

        assertThat(form.id()).isEqualTo(jdbc.queryForObject("SELECT id FROM data_template WHERE uid = :uid",
            Map.of("uid", FORM_UID), String.class));
        assertThat(form.versions()).extracting(FormVersionDefinition::number).containsExactly(2, 1);
        FormVersionDefinition v2 = form.versions().get(0);
        assertThat(v2.sections()).extracting(SectionDefinition::path, SectionDefinition::repeating)
            .containsExactlyInAnyOrder(tuple("main", false), tuple("main.rows", true),
                tuple("main.rows.parts", true));
        assertThat(v2.fields()).extracting(FieldDefinition::id, FieldDefinition::path, FieldDefinition::valueType)
            .containsExactlyInAnyOrder(
                tuple(DATA_ELEMENTS.get("s1DeTitle01"), "s1_title", ValueType.Text),
                tuple(DATA_ELEMENTS.get("s1DeCount01"), "main.rows.s1_count", ValueType.Integer),
                tuple(DATA_ELEMENTS.get("s1DePart001"), "main.rows.parts.s1_part", ValueType.Text));
        assertThat(form.versions().get(1).fields()).extracting(FieldDefinition::path)
            .containsExactlyInAnyOrder("main.s1_title", "main.s1_note", "main.rows.s1_count");
    }

    @Test
    void aStoredSubmissionNormalizesAgainstTheReadDefinition() {
        FormDefinition form = reader.read(FORM_UID).orElseThrow();
        String pinned = form.versions().get(0).uid();

        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(
            "01JS1SB0000000000000000001", pinned, json.valueToTree(Map.of(
                "s1_title", "T",
                "main", Map.of(
                    "s1_note", "kept through version 1",
                    "rows", List.of(Map.of("_id", "01JS1RW0000000000000000001", "s1_count", 3,
                        "parts", List.of(Map.of("s1_part", "p")))))))),
            form);

        assertThat(normalized.values()).extracting(v -> v.fieldId(), v -> v.fromPinnedVersion())
            .containsExactly(
                tuple(DATA_ELEMENTS.get("s1DeNote001"), false),
                tuple(DATA_ELEMENTS.get("s1DeTitle01"), true));
        assertThat(normalized.rows())
            .extracting(NormalizedRow::id, NormalizedRow::keyKind, NormalizedRow::parentRowId, NormalizedRow::sectionPath)
            .containsExactly(
                tuple("01JS1RW0000000000000000001", RowKeyKind.CLIENT, null, "main.rows"),
                tuple(normalized.rows().get(1).id(), RowKeyKind.DERIVED, "01JS1RW0000000000000000001",
                    "main.rows.parts"));
        assertThat(normalized.unmapped()).isEmpty();
        assertThat(normalized.flags()).isEmpty();
    }

    @Test
    void severalFormsAreReadTogetherAndUnknownOnesAreAbsent() {
        assertThat(reader.read(Set.of(FORM_UID, "s1NoSuchFrm"))).containsOnlyKeys(FORM_UID);
        assertThat(reader.read("s1NoSuchFrm")).isEmpty();
        assertThat(reader.read(Set.of())).isEmpty();
    }

    private void dataElement(String uid, String name, String type) {
        jdbc.update("""
                INSERT INTO data_element (id, uid, code, name, type, created_by)
                VALUES (:id, :uid, :uid, :name, :type, 's1-test')""",
            Map.of("id", DATA_ELEMENTS.get(uid), "uid", uid, "name", name, "type", type));
    }

    private void publish(String template) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/dataFormTemplates")
                .header("Authorization", "Bearer " + JwtAuthenticationTestUtils.createValidTokenForUser(jwtKey, "admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(template))
            .andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
    }
}
