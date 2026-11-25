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
import org.nmcpye.datarun.submission.normalized.RowIdOrigin;
import org.nmcpye.datarun.submission.normalized.SectionDefinition;
import org.nmcpye.datarun.submission.normalized.StoredSubmission;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;
import org.nmcpye.datarun.submission.normalized.UnmappedValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Form definitions stored the way the template route stores them (section paths of names, field paths ending with
 * the data element's uid, a field outside every section), read back for the normalizer on the real schema
 * (Testcontainers, Liquibase). It stores them with plain SQL rather than through the route, so it needs no access
 * token. Ids and values are invented.
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormDefinitionJdbcReaderIT {

    private static final String FORM_UID = "nzFormNorm1";
    private static final String FORM_ID = "01JS1FM0000000000000000001";
    private static final Map<String, String> DATA_ELEMENTS = Map.of(
        "nzDeTitle01", "01JS1DE0000000000000000001",
        "nzDeCount01", "01JS1DE0000000000000000002",
        "nzDeNote001", "01JS1DE0000000000000000004");

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
            dataElement("nzDeTitle01", "nz_title", "Text");
            dataElement("nzDeCount01", "nz_count", "Integer");
            dataElement("nzDeNote001", "nz_note", "LongText");
            // no data element nzDePart001 is stored: version 2's field names one that is gone
            jdbc.update("""
                    INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
                    VALUES (:id, :uid, :uid, 'nz normalized form', 'nzFormVer02', 2, 'nz-test')""",
                Map.of("id", FORM_ID, "uid", FORM_UID));
            version("01JS1FV0000000000000000001", "nzFormVer01", 1, """
                [{"id": "main", "name": "main", "path": "main", "repeatable": false},
                 {"id": "rows", "name": "rows", "path": "main.rows", "parent": "main", "repeatable": true}]""", """
                [{"id": "nzDeTitle01", "name": "nz_title", "path": "main.nzDeTitle01", "parent": "main", "type": "Text"},
                 {"id": "nzDeNote001", "name": "nz_note", "path": "main.nzDeNote001", "parent": "main",
                  "type": "LongText"},
                 {"id": "nzDeCount01", "name": "nz_count", "path": "main.rows.nzDeCount01", "parent": "rows",
                  "type": "Integer"}]""");
            version("01JS1FV0000000000000000002", "nzFormVer02", 2, """
                [{"id": "main", "name": "main", "path": "main", "repeatable": false},
                 {"id": "rows", "name": "rows", "path": "main.rows", "parent": "main", "repeatable": true},
                 {"name": "parts", "path": "main.rows.parts", "parent": "rows", "repeatable": true}]""", """
                [{"id": "nzDeTitle01", "name": "nz_title", "path": "nzDeTitle01", "type": "Text"},
                 {"id": "nzDeCount01", "name": "nz_count", "path": "main.rows.nzDeCount01", "parent": "rows",
                  "type": "Integer"},
                 {"id": "nzDePart001", "name": "nz_part", "path": "main.rows.parts.nzDePart001", "parent": "parts",
                  "type": "Text"}]""");
        });
    }

    @Test
    void everyVersionIsReadWithDataPathsAndDataElementIds() {
        FormDefinition form = reader.read(FORM_UID).orElseThrow().definition();

        assertThat(form.id()).isEqualTo(FORM_ID);
        assertThat(form.versions()).extracting(FormVersionDefinition::number).containsExactly(2, 1);
        FormVersionDefinition v2 = form.versions().get(0);
        assertThat(v2.sections()).extracting(SectionDefinition::path, SectionDefinition::repeating)
            .containsExactlyInAnyOrder(tuple("main", false), tuple("main.rows", true),
                tuple("main.rows.parts", true));
        assertThat(v2.fields()).extracting(FieldDefinition::id, FieldDefinition::path, FieldDefinition::valueType)
            .containsExactlyInAnyOrder(
                tuple(DATA_ELEMENTS.get("nzDeTitle01"), "nz_title", ValueType.Text),
                tuple(DATA_ELEMENTS.get("nzDeCount01"), "main.rows.nz_count", ValueType.Integer),
                // its data element is gone: the field is kept without an id, and nothing fails
                tuple(null, "main.rows.parts.nz_part", ValueType.Text));
        assertThat(form.versions().get(1).fields()).extracting(FieldDefinition::path)
            .containsExactlyInAnyOrder("main.nz_title", "main.nz_note", "main.rows.nz_count");
    }

    @Test
    void aStoredSubmissionNormalizesAgainstTheReadDefinition() throws Exception {
        FormDefinition form = reader.read(FORM_UID).orElseThrow().definition();

        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(
            "01JS1SB0000000000000000001", "nzFormVer02", json.writeValueAsString(Map.of(
                "nz_title", "T",
                "main", Map.of(
                    "nz_note", "kept through version 1",
                    "rows", List.of(Map.of("_id", "01JS1RW0000000000000000001", "nz_count", 3,
                        "parts", List.of(Map.of("nz_part", "p")))))))),
            form);

        assertThat(normalized.formId()).isEqualTo(FORM_ID);
        assertThat(normalized.formVersionId()).isEqualTo("01JS1FV0000000000000000002");
        assertThat(normalized.values()).extracting(v -> v.fieldId(), v -> v.fromPinnedVersion())
            .containsExactly(
                tuple(DATA_ELEMENTS.get("nzDeNote001"), false),
                tuple(DATA_ELEMENTS.get("nzDeTitle01"), true));
        assertThat(normalized.rows())
            .extracting(NormalizedRow::id, NormalizedRow::idOrigin, NormalizedRow::parentRowId, NormalizedRow::sectionPath)
            .containsExactly(
                tuple("01JS1RW0000000000000000001", RowIdOrigin.STORED, null, "main.rows"),
                tuple(normalized.rows().get(1).id(), RowIdOrigin.DERIVED, "01JS1RW0000000000000000001",
                    "main.rows.parts"));
        assertThat(normalized.rows().get(1).unmapped()).extracting(UnmappedValue::path)
            .containsExactly("main.rows.parts.nz_part");
        assertThat(normalized.flags()).containsOnlyKeys(NormalizationFlag.FIELD_WITHOUT_ID);
    }

    @Test
    void severalFormsAreReadTogetherAndUnknownOnesAreAbsent() {
        assertThat(reader.read(Set.of(FORM_UID, "nzNoSuchFrm"))).containsOnlyKeys(FORM_UID);
        assertThat(reader.read("nzNoSuchFrm")).isEmpty();
        assertThat(reader.read(Set.of())).isEmpty();
        assertThat(reader.changeTokens(Set.of("nzNoSuchFrm"))).isEmpty();
    }

    @Test
    void theReaderNeverWrites() {
        // in a read-only transaction PostgreSQL refuses every write, so a read that succeeds there wrote nothing
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);

        String token = readOnly.execute(status -> reader.read(FORM_UID).orElseThrow().changeToken());
        Map<String, String> tokens = readOnly.execute(status -> reader.changeTokens(Set.of(FORM_UID)));
        assertThat(token).isNotBlank();
        assertThat(tokens).containsOnlyKeys(FORM_UID);
        assertThatThrownBy(() -> readOnly.executeWithoutResult(status -> jdbc.update(
            "UPDATE data_element SET name = name WHERE uid = :uid", Map.of("uid", "nzDeTitle01"))))
            .as("the transaction really is read-only").isInstanceOf(DataAccessException.class);
    }

    @Test
    void theChangeTokenMovesWhenAVersionOrADataElementChanges() {
        String token = reader.read(FORM_UID).orElseThrow().changeToken();
        assertThat(reader.changeTokens(Set.of(FORM_UID))).containsEntry(FORM_UID, token);

        inTransaction(() -> version("01JS1FV0000000000000000003", "nzFormVer03", 3, "[]",
            "[{\"id\": \"nzDeTitle01\", \"name\": \"nz_title\", \"path\": \"nzDeTitle01\", \"type\": \"Text\"}]"));
        String added = currentToken();
        inTransaction(() -> jdbc.update("UPDATE data_template_version SET release_notes = 'rewritten' WHERE id = :id",
            Map.of("id", "01JS1FV0000000000000000003")));
        String rewritten = currentToken();
        inTransaction(() -> jdbc.update("DELETE FROM data_template_version WHERE id = :id",
            Map.of("id", "01JS1FV0000000000000000003")));
        String deleted = currentToken();
        inTransaction(() -> jdbc.update("UPDATE data_element SET short_name = 'renamed' WHERE uid = :uid",
            Map.of("uid", "nzDeTitle01")));
        String elementChanged = currentToken();
        // version 2 names a data element that does not exist: create it, then delete it again
        inTransaction(() -> jdbc.update("""
                INSERT INTO data_element (id, uid, code, name, type, created_by)
                VALUES ('01JS1DE0000000000000000003', 'nzDePart001', 'nzDePart001', 'nz_part', 'Text', 'nz-test')""",
            Map.of()));
        String elementCreated = currentToken();
        inTransaction(() -> jdbc.update("DELETE FROM data_element WHERE uid = 'nzDePart001'", Map.of()));
        String elementDeletedAgain = currentToken();

        assertThat(List.of(token, added, rewritten, elementChanged, elementCreated)).doesNotHaveDuplicates();
        assertThat(deleted).as("back to the same versions and elements").isEqualTo(token);
        assertThat(elementDeletedAgain).as("back to the same elements").isEqualTo(elementChanged);
        assertThat(reader.read(FORM_UID).orElseThrow().changeToken()).isEqualTo(elementChanged);
    }

    @Test
    void aFormWhoseVersionsAreAllDeletedHasNoToken_whichCallersTreatAsAChange() {
        String formUid = "nzFormGone1";
        String formId = "01JS1FG0000000000000000001";
        inTransaction(() -> {
            jdbc.update("""
                    INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
                    VALUES (:id, :uid, :uid, 'nz form with no versions left', 'nzGoneVer01', 1, 'nz-test')""",
                Map.of("id", formId, "uid", formUid));
            version(formUid, formId, "01JS1FG0000000000000000002", "nzGoneVer01", 1, "[]", "[]");
        });
        assertThat(reader.changeTokens(Set.of(formUid))).containsOnlyKeys(formUid);

        inTransaction(() -> jdbc.update("DELETE FROM data_template_version WHERE data_template_id = :id",
            Map.of("id", formId)));
        try {
            assertThat(reader.changeTokens(Set.of(formUid))).as("no token: a change").isEmpty();
            assertThat(reader.read(formUid)).isEmpty();
        } finally {
            inTransaction(() -> jdbc.update("DELETE FROM data_template WHERE id = :id", Map.of("id", formId)));
        }
    }

    private String currentToken() {
        return reader.changeTokens(Set.of(FORM_UID)).get(FORM_UID);
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private void dataElement(String uid, String name, String type) {
        jdbc.update("""
                INSERT INTO data_element (id, uid, code, name, type, created_by)
                VALUES (:id, :uid, :uid, :name, :type, 'nz-test')""",
            Map.of("id", DATA_ELEMENTS.get(uid), "uid", uid, "name", name, "type", type));
    }

    private void version(String id, String uid, int number, String sections, String fields) {
        version(FORM_UID, FORM_ID, id, uid, number, sections, fields);
    }

    private void version(String formUid, String formId, String id, String uid, int number, String sections,
                         String fields) {
        jdbc.update("""
                INSERT INTO data_template_version
                    (id, uid, template_uid, version_number, sections, fields, data_template_id, created_by)
                VALUES (:id, :uid, :formUid, :number, CAST(:sections AS jsonb), CAST(:fields AS jsonb), :formId,
                        'nz-test')""",
            Map.of("id", id, "uid", uid, "formUid", formUid, "number", number, "sections", sections,
                "fields", fields, "formId", formId));
    }
}
