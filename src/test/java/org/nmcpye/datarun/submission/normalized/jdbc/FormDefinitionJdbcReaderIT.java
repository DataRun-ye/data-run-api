package org.nmcpye.datarun.submission.normalized.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.submission.normalized.FieldDefinition;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;
import org.nmcpye.datarun.submission.normalized.NormalizationFlag;
import org.nmcpye.datarun.submission.normalized.NormalizedRow;
import org.nmcpye.datarun.submission.normalized.NormalizedSubmission;
import org.nmcpye.datarun.submission.normalized.RowKeyKind;
import org.nmcpye.datarun.submission.normalized.SectionDefinition;
import org.nmcpye.datarun.submission.normalized.StoredSubmission;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;
import org.nmcpye.datarun.submission.normalized.UnmappedValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Form definitions stored the way the template route stores them (section paths of names, field paths ending with
 * the data element's uid, a field outside every section), read back for the normalizer on the real schema
 * (Testcontainers, Liquibase). The staging agreement run read all 209 stored versions the same way. It stores them
 * with plain SQL rather than through the route, so it needs no access token. Ids and values are invented.
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormDefinitionJdbcReaderIT {

    private static final String FORM_UID = "s1FormNorm1";
    private static final String FORM_ID = "01JS1FM0000000000000000001";
    private static final Map<String, String> DATA_ELEMENTS = Map.of(
        "s1DeTitle01", "01JS1DE0000000000000000001",
        "s1DeCount01", "01JS1DE0000000000000000002",
        "s1DeNote001", "01JS1DE0000000000000000004");

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FormDefinitionJdbcReader reader;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void storeTwoVersions() {
        Integer present = jdbc.queryForObject("SELECT count(*) FROM data_template WHERE uid = :uid",
            Map.of("uid", FORM_UID), Integer.class);
        if (present != null && present > 0) {
            return;
        }
        // the application's connections do not auto-commit
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            dataElement("s1DeTitle01", "s1_title", "Text");
            dataElement("s1DeCount01", "s1_count", "Integer");
            dataElement("s1DeNote001", "s1_note", "LongText");
            // no data element s1DePart001 is stored: version 2's field names one that is gone
            jdbc.update("""
                    INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
                    VALUES (:id, :uid, :uid, 's1 normalized form', 's1FormVer02', 2, 's1-test')""",
                Map.of("id", FORM_ID, "uid", FORM_UID));
            version("01JS1FV0000000000000000001", "s1FormVer01", 1, """
                [{"id": "main", "name": "main", "path": "main", "repeatable": false},
                 {"id": "rows", "name": "rows", "path": "main.rows", "parent": "main", "repeatable": true}]""", """
                [{"id": "s1DeTitle01", "name": "s1_title", "path": "main.s1DeTitle01", "parent": "main", "type": "Text"},
                 {"id": "s1DeNote001", "name": "s1_note", "path": "main.s1DeNote001", "parent": "main",
                  "type": "LongText"},
                 {"id": "s1DeCount01", "name": "s1_count", "path": "main.rows.s1DeCount01", "parent": "rows",
                  "type": "Integer"}]""");
            version("01JS1FV0000000000000000002", "s1FormVer02", 2, """
                [{"id": "main", "name": "main", "path": "main", "repeatable": false},
                 {"id": "rows", "name": "rows", "path": "main.rows", "parent": "main", "repeatable": true},
                 {"name": "parts", "path": "main.rows.parts", "parent": "rows", "repeatable": true}]""", """
                [{"id": "s1DeTitle01", "name": "s1_title", "path": "s1DeTitle01", "type": "Text"},
                 {"id": "s1DeCount01", "name": "s1_count", "path": "main.rows.s1DeCount01", "parent": "rows",
                  "type": "Integer"},
                 {"id": "s1DePart001", "name": "s1_part", "path": "main.rows.parts.s1DePart001", "parent": "parts",
                  "type": "Text"}]""");
        });
    }

    @Test
    void everyVersionIsReadWithDataPathsAndDataElementIds() {
        FormDefinition form = reader.read(FORM_UID).orElseThrow();

        assertThat(form.id()).isEqualTo(FORM_ID);
        assertThat(form.versions()).extracting(FormVersionDefinition::number).containsExactly(2, 1);
        FormVersionDefinition v2 = form.versions().get(0);
        assertThat(v2.sections()).extracting(SectionDefinition::path, SectionDefinition::repeating)
            .containsExactlyInAnyOrder(tuple("main", false), tuple("main.rows", true),
                tuple("main.rows.parts", true));
        assertThat(v2.fields()).extracting(FieldDefinition::id, FieldDefinition::path, FieldDefinition::valueType)
            .containsExactlyInAnyOrder(
                tuple(DATA_ELEMENTS.get("s1DeTitle01"), "s1_title", ValueType.Text),
                tuple(DATA_ELEMENTS.get("s1DeCount01"), "main.rows.s1_count", ValueType.Integer),
                // its data element is gone: the field is kept without an id, and nothing fails
                tuple(null, "main.rows.parts.s1_part", ValueType.Text));
        assertThat(form.versions().get(1).fields()).extracting(FieldDefinition::path)
            .containsExactlyInAnyOrder("main.s1_title", "main.s1_note", "main.rows.s1_count");
    }

    @Test
    void aStoredSubmissionNormalizesAgainstTheReadDefinition() {
        FormDefinition form = reader.read(FORM_UID).orElseThrow();

        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(
            "01JS1SB0000000000000000001", "s1FormVer02", json.valueToTree(Map.of(
                "s1_title", "T",
                "main", Map.of(
                    "s1_note", "kept through version 1",
                    "rows", List.of(Map.of("_id", "01JS1RW0000000000000000001", "s1_count", 3,
                        "parts", List.of(Map.of("s1_part", "p")))))))),
            form);

        assertThat(normalized.formId()).isEqualTo(FORM_ID);
        assertThat(normalized.formVersionId()).isEqualTo("01JS1FV0000000000000000002");
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
        assertThat(normalized.rows().get(1).unmapped()).extracting(UnmappedValue::path)
            .containsExactly("main.rows.parts.s1_part");
        assertThat(normalized.flags()).containsOnlyKeys(NormalizationFlag.FIELD_WITHOUT_ID);
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

    private void version(String id, String uid, int number, String sections, String fields) {
        jdbc.update("""
                INSERT INTO data_template_version
                    (id, uid, template_uid, version_number, sections, fields, data_template_id, created_by)
                VALUES (:id, :uid, :formUid, :number, CAST(:sections AS jsonb), CAST(:fields AS jsonb), :formId,
                        's1-test')""",
            Map.of("id", id, "uid", uid, "formUid", FORM_UID, "number", number, "sections", sections,
                "fields", fields, "formId", FORM_ID));
    }
}
