package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.security.jwt.JwtAuthenticationTestUtils;
import org.nmcpye.datarun.submission.feed.FeedActivation;
import org.nmcpye.datarun.submission.normalized.DerivedIds;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The dictionary in the running application with the switch on: written at start, rewritten at the next check after a
 * version is published through the authoring route, and after every kind of change the other paths make to the
 * stored definitions (a version added, rewritten or deleted; a data element or an option set its fields name deleted
 * or created), never when nothing changed, and deferred while another session rewrites it. After every check, the two
 * tables hold exactly the dictionary of the stored definitions. Ids, names and labels are invented.
 */
@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "datarun.activation.submission-feed=true",
    "datarun.feed.dictionary-check-delay=PT1H"})
class DictionaryPublisherIT {

    @Autowired
    private DictionaryPublisher publisher;

    @Autowired
    private ActivationSwitches switches;

    @Autowired
    private FormDefinitionJdbcReader reader;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
        new com.fasterxml.jackson.databind.ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private void update(String sql, Map<String, ?> parameters) {
        inTransaction(() -> jdbc.update(sql, parameters));
    }

    /** Both tables, as records, against the dictionary of every stored definition read now. */
    private void assertTheDictionaryIsTheStoredDefinitions() {
        FormDictionary expected = new TransactionTemplate(transactionManager).execute(status -> FormDictionary.of(
            reader.readAll().values().stream().map(FormDefinitionJdbcReader.Snapshot::definition).toList()));
        List<FormDictionary.FieldRecord> fields = jdbc.query("""
                SELECT form_id, form_version_id, form_version_number, field_id, section_id, repeat_section_id, name,
                       label::text AS label, value_type, option_set_id, mandatory, sort_order
                FROM feed.form_field""", Map.of(),
            (rs, n) -> new FormDictionary.FieldRecord(rs.getString("form_id"), rs.getString("form_version_id"),
                rs.getInt("form_version_number"), rs.getString("field_id"), rs.getString("section_id"),
                rs.getString("repeat_section_id"), rs.getString("name"), normalizedJson(rs.getString("label")),
                rs.getString("value_type"), rs.getString("option_set_id"), (Boolean) rs.getObject("mandatory"),
                (Integer) rs.getObject("sort_order")));
        List<FormDictionary.SectionRecord> sections = jdbc.query("""
                SELECT form_id, form_version_id, form_version_number, section_id, parent_section_id, name,
                       label::text AS label, repeatable, sort_order
                FROM feed.form_section""", Map.of(),
            (rs, n) -> new FormDictionary.SectionRecord(rs.getString("form_id"), rs.getString("form_version_id"),
                rs.getInt("form_version_number"), rs.getString("section_id"), rs.getString("parent_section_id"),
                rs.getString("name"), normalizedJson(rs.getString("label")), rs.getBoolean("repeatable"),
                (Integer) rs.getObject("sort_order")));
        assertThat(fields).containsExactlyInAnyOrderElementsOf(expected.fields().stream()
            .map(field -> new FormDictionary.FieldRecord(field.formId(), field.formVersionId(),
                field.formVersionNumber(), field.fieldId(), field.sectionId(), field.repeatSectionId(), field.name(),
                normalizedJson(field.label()), field.valueType(), field.optionSetId(), field.mandatory(),
                field.sortOrder())).toList());
        assertThat(sections).containsExactlyInAnyOrderElementsOf(expected.sections().stream()
            .map(section -> new FormDictionary.SectionRecord(section.formId(), section.formVersionId(),
                section.formVersionNumber(), section.sectionId(), section.parentSectionId(), section.name(),
                normalizedJson(section.label()), section.repeatable(), section.sortOrder())).toList());
        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT published_at) FROM (SELECT published_at FROM feed.form_field
                                                          UNION ALL SELECT published_at FROM feed.form_section) a""",
            Map.of(), Long.class)).as("written in one transaction").isLessThanOrEqualTo(1);
    }

    /** JSON text without its spacing, so that labels compare as JSON; both sides keep jsonb's order of keys. */
    private static String normalizedJson(String text) {
        try {
            return text == null ? null : JSON.readTree(text).toString();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> fieldsOf(String formUid) {
        return jdbc.queryForList("""
                SELECT f.form_version_number || ' ' || f.name || ' ' || coalesce(f.option_set_id, '-')
                FROM feed.form_field f JOIN data_template t ON t.id = f.form_id
                WHERE t.uid = :uid ORDER BY 1""", Map.of("uid", formUid), String.class);
    }

    private void dataElement(String id, String uid, String name, String type) {
        update("""
                INSERT INTO data_element (id, uid, code, name, type, created_by)
                VALUES (:id, :uid, :uid, :name, :type, 'fd-test')""",
            Map.of("id", id, "uid", uid, "name", name, "type", type));
    }

    private void version(String formUid, String formId, String id, String uid, int number, String sections,
                         String fields) {
        update("""
                INSERT INTO data_template_version
                    (id, uid, template_uid, version_number, sections, fields, data_template_id, created_by)
                VALUES (:id, :uid, :formUid, :number, CAST(:sections AS jsonb), CAST(:fields AS jsonb), :formId,
                        'fd-test')""",
            Map.of("id", id, "uid", uid, "formUid", formUid, "number", number, "sections", sections,
                "fields", fields, "formId", formId));
    }

    @Test
    void theSwitchIsOnAndTheStartPublishedTheStoredDefinitions() {
        assertThat(switches.isOn(FeedActivation.SUBMISSION_FEED)).isTrue();
        publisher.publishIfChanged();
        assertTheDictionaryIsTheStoredDefinitions();
        assertThat(publisher.publishIfChanged()).as("nothing changed since").isFalse();
    }

    @Test
    void aVersionPublishedThroughTheAuthoringRouteIsPublishedAtTheNextCheck() throws Exception {
        dataElement("01JFDDE0000000000000000001", "fdDeTitle01", "fd_title", "Text");
        dataElement("01JFDDE0000000000000000002", "fdDeCount01", "fd_count", "SelectOne");
        update("""
                INSERT INTO option_set (id, uid, code, name, created_by)
                VALUES ('01JFDOS0000000000000000001', 'fdOsCount01', 'fdOsCount01', 'fd counts', 'fd-test')""",
            Map.of());
        publisher.publishIfChanged();

        mvc.perform(post("/api/v1/dataFormTemplates")
                .header("Authorization", "Bearer " + JwtAuthenticationTestUtils.createValidTokenForUser(jwtKey, "admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"uid": "fdFormDic01", "name": "fd dictionary form",
                     "fields": [
                       {"id": "fdDeTitle01", "parent": "main", "type": "Text", "mandatory": true, "order": 1,
                        "label": {"en": "Title"}},
                       {"id": "fdDeCount01", "parent": "rows", "type": "SelectOne", "optionSet": "fdOsCount01",
                        "order": 2}],
                     "sections": [{"name": "main", "order": 1},
                                  {"name": "rows", "parent": "main", "repeatable": true, "order": 2}]}"""))
            .andExpect(status().isOk());

        assertThat(publisher.publishIfChanged()).isTrue();
        assertTheDictionaryIsTheStoredDefinitions();
        String formId = jdbc.queryForObject("SELECT id FROM data_template WHERE uid = 'fdFormDic01'", Map.of(),
            String.class);
        assertThat(jdbc.query("""
                SELECT name, section_id, repeat_section_id, value_type, option_set_id, mandatory, sort_order,
                       label ->> 'en' AS label
                FROM feed.form_field WHERE form_id = :form ORDER BY sort_order""", Map.of("form", formId),
            (rs, n) -> tuple(rs.getString("name"), rs.getString("section_id"), rs.getString("repeat_section_id"),
                rs.getString("value_type"), rs.getString("option_set_id"), rs.getObject("mandatory"),
                rs.getObject("sort_order"), rs.getString("label"))))
            .containsExactly(
                tuple("fd_title", DerivedIds.sectionId(formId, "main"), null, "Text", null, true, 1, "Title"),
                tuple("fd_count", DerivedIds.sectionId(formId, "main.rows"), DerivedIds.sectionId(formId, "main.rows"),
                    "SelectOne", "01JFDOS0000000000000000001", false, 2, null));
        assertThat(jdbc.queryForList("""
                SELECT section_id || ' ' || coalesce(parent_section_id, '-') || ' ' || repeatable
                FROM feed.form_section WHERE form_id = :form ORDER BY sort_order""", Map.of("form", formId),
            String.class)).containsExactly(
            DerivedIds.sectionId(formId, "main") + " - false",
            DerivedIds.sectionId(formId, "main.rows") + " " + DerivedIds.sectionId(formId, "main") + " true");

        assertThat(publisher.publishIfChanged()).as("nothing changed since").isFalse();
    }

    @Test
    void everyKindOfChangeToTheStoredDefinitionsIsPublishedAtTheNextCheck() {
        String formUid = "fdFormDic02";
        String formId = "01JFDFM0000000000000000002";
        dataElement("01JFDDE0000000000000000011", "fdDeOne0001", "fd_one", "Text");
        dataElement("01JFDDE0000000000000000012", "fdDeTwo0001", "fd_two", "Integer");
        update("""
                INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
                VALUES (:id, :uid, :uid, 'fd changing form', 'fdChgVer001', 1, 'fd-test')""",
            Map.of("id", formId, "uid", formUid));
        version(formUid, formId, "01JFDFV0000000000000000021", "fdChgVer001", 1, "[]",
            "[{\"id\": \"fdDeOne0001\", \"name\": \"fd_one\", \"path\": \"fdDeOne0001\", \"type\": \"Text\"}]");
        assertThat(publisher.publishIfChanged()).isTrue();
        assertThat(fieldsOf(formUid)).containsExactly("1 fd_one -");

        // a version published (the authoring route, or a migration)
        version(formUid, formId, "01JFDFV0000000000000000022", "fdChgVer002", 2, "[]", """
            [{"id": "fdDeOne0001", "name": "fd_one", "path": "fdDeOne0001", "type": "Text"},
             {"id": "fdDeTwo0001", "name": "fd_two", "path": "fdDeTwo0001", "type": "Integer",
              "optionSet": "fdOsLater01"}]""");
        assertThat(publisher.publishIfChanged()).isTrue();
        assertThat(fieldsOf(formUid)).containsExactly("1 fd_one -", "2 fd_one -", "2 fd_two -");

        // a version rewritten (the generic PUT)
        update("""
                UPDATE data_template_version
                SET fields = jsonb_set(fields, '{0,name}', '"fd_one_renamed"')
                WHERE id = '01JFDFV0000000000000000022'""", Map.of());
        assertThat(publisher.publishIfChanged()).isTrue();
        assertThat(fieldsOf(formUid)).containsExactly("1 fd_one -", "2 fd_one_renamed -", "2 fd_two -");

        // a version deleted (the generic DELETE)
        update("DELETE FROM data_template_version WHERE id = '01JFDFV0000000000000000021'", Map.of());
        assertThat(publisher.publishIfChanged()).isTrue();
        assertThat(fieldsOf(formUid)).containsExactly("2 fd_one_renamed -", "2 fd_two -");

        // a data element a field names deleted, then created again; an option set it names created
        update("DELETE FROM data_element WHERE uid = 'fdDeTwo0001'", Map.of());
        assertThat(publisher.publishIfChanged()).isTrue();
        assertThat(fieldsOf(formUid)).containsExactly("2 fd_one_renamed -");
        dataElement("01JFDDE0000000000000000013", "fdDeTwo0001", "fd_two", "Integer");
        update("""
                INSERT INTO option_set (id, uid, code, name, created_by)
                VALUES ('01JFDOS0000000000000000002', 'fdOsLater01', 'fdOsLater01', 'fd later', 'fd-test')""",
            Map.of());
        assertThat(publisher.publishIfChanged()).isTrue();
        assertThat(fieldsOf(formUid))
            .containsExactly("2 fd_one_renamed -", "2 fd_two 01JFDOS0000000000000000002");
        assertTheDictionaryIsTheStoredDefinitions();

        // the template's own row gives the dictionary only its id: a soft delete changes nothing
        update("UPDATE data_template SET deleted = true WHERE id = :id", Map.of("id", formId));
        assertThat(publisher.publishIfChanged()).isFalse();
        assertThat(fieldsOf(formUid)).hasSize(2);
    }

    @Test
    void whileAnotherSessionRewritesTheDictionaryTheCheckLeavesItAndTriesAgainLater() throws Exception {
        String formUid = "fdFormDic03";
        String formId = "01JFDFM0000000000000000003";
        dataElement("01JFDDE0000000000000000021", "fdDeThree01", "fd_three", "Text");
        publisher.publishIfChanged();
        List<String> before = jdbc.queryForList("SELECT form_version_id || field_id FROM feed.form_field", Map.of(),
            String.class);

        // a session of its own, outside the application's pool
        try (Connection other = DriverManager.getConnection(url, username, password)) {
            other.setAutoCommit(false);
            try (PreparedStatement lock = other.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                lock.setLong(1, DictionaryPublisher.LOCK_KEY);
                lock.execute();
            }
            update("""
                    INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
                    VALUES (:id, :uid, :uid, 'fd locked form', 'fdLckVer001', 1, 'fd-test')""",
                Map.of("id", formId, "uid", formUid));
            version(formUid, formId, "01JFDFV0000000000000000031", "fdLckVer001", 1, "[]",
                "[{\"id\": \"fdDeThree01\", \"name\": \"fd_three\", \"path\": \"fdDeThree01\", \"type\": \"Text\"}]");

            assertThat(publisher.publishIfChanged()).isFalse();
            assertThat(jdbc.queryForList("SELECT form_version_id || field_id FROM feed.form_field", Map.of(),
                String.class)).containsExactlyInAnyOrderElementsOf(before);
            other.rollback();
        }

        assertThat(publisher.publishIfChanged()).isTrue();
        assertThat(fieldsOf(formUid)).containsExactly("1 fd_three -");
        assertTheDictionaryIsTheStoredDefinitions();
    }
}
