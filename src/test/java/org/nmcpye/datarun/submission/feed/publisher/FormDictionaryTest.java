package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.submission.normalized.DerivedIds;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The dictionary's records from stored definitions: one per section and field placement of every version, the
 * section that holds each and the repeating section whose rows hold a field's values, the attributes as stored, only
 * fields with a data element, a key given twice kept once, and a fixed order. Ids and names are invented.
 */
class FormDictionaryTest {

    private static final String FORM = "01JFD000000000000000000001";
    private static final String OTHER_FORM = "01JFD000000000000000000000";
    private static final Map<String, String> DATA_ELEMENTS = Map.of(
        "deUidTitle1", "01JDE000000000000000000001",
        "deUidName01", "01JDE000000000000000000002",
        "deUidCount1", "01JDE000000000000000000003",
        "deUidNote01", "01JDE000000000000000000004",
        "deUidSub001", "01JDE000000000000000000005");
    private static final Map<String, String> OPTION_SETS = Map.of("osUidCount1", "01JOS000000000000000000001");

    private static JsonNode json(String singleQuoted) {
        try {
            return new ObjectMapper().readTree(singleQuoted.replace('\'', '"'));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static FormVersionDefinition version(String formId, String id, int number, String sections,
                                                 String fields) {
        return FormVersionDefinition.parse(formId, id, "fvUid" + id.substring(id.length() - 6), number,
            json(sections), json(fields), DATA_ELEMENTS::get, OPTION_SETS::get);
    }

    private static String section(String path) {
        return DerivedIds.sectionId(FORM, path);
    }

    private static final String SECTIONS = """
        [{'name': 'main', 'path': 'main', 'order': 1, 'label': {'en': 'Main'}},
         {'name': 'rows', 'path': 'main.rows', 'repeatable': true, 'order': 2},
         {'name': 'details', 'path': 'main.rows.details'},
         {'name': 'subrows', 'path': 'main.rows.subrows', 'repeatable': true},
         {'name': 'child', 'path': 'orphan.child'}]""";

    private static final String FIELDS = """
        [{'id': 'deUidTitle1', 'name': 'title', 'path': 'deUidTitle1', 'type': 'Text', 'mandatory': true,
          'order': 1, 'label': {'en': 'Title', 'ar': 'Unwan'}},
         {'id': 'deUidName01', 'name': 'name', 'path': 'main.deUidName01', 'type': 'Text'},
         {'id': 'deUidCount1', 'name': 'count', 'path': 'main.rows.deUidCount1', 'type': 'SelectOne',
          'optionSet': 'osUidCount1', 'mandatory': false},
         {'id': 'deUidNote01', 'name': 'note', 'path': 'main.rows.details.deUidNote01', 'type': 'LongText'},
         {'id': 'deUidSub001', 'name': 'sub', 'path': 'main.rows.subrows.deUidSub001', 'type': 'Integer'},
         {'id': 'deUidGone01', 'name': 'gone', 'path': 'main.deUidGone01', 'type': 'Text'}]""";

    @Test
    void eachFieldPlacementAndSectionIsOneRecordWithItsHoldingSections() {
        FormDictionary dictionary = FormDictionary.of(List.of(FormDefinition.of(FORM,
            List.of(version(FORM, "01JFV000000000000000000001", 1, SECTIONS, FIELDS)))));

        assertThat(dictionary.sections())
            .extracting(FormDictionary.SectionRecord::sectionId, FormDictionary.SectionRecord::parentSectionId,
                FormDictionary.SectionRecord::name, FormDictionary.SectionRecord::repeatable,
                FormDictionary.SectionRecord::sortOrder, FormDictionary.SectionRecord::label)
            .containsExactly(
                tuple(section("main"), null, "main", false, 1, "{\"en\":\"Main\"}"),
                tuple(section("main.rows"), section("main"), "rows", true, 2, null),
                tuple(section("main.rows.details"), section("main.rows"), "details", false, null, null),
                tuple(section("main.rows.subrows"), section("main.rows"), "subrows", true, null, null),
                // its parent path is no section of the version: a section outside every other
                tuple(section("orphan.child"), null, "child", false, null, null));
        assertThat(dictionary.fields())
            .extracting(FormDictionary.FieldRecord::fieldId, FormDictionary.FieldRecord::name,
                FormDictionary.FieldRecord::sectionId, FormDictionary.FieldRecord::repeatSectionId)
            .containsExactly(
                tuple("01JDE000000000000000000001", "title", null, null),
                tuple("01JDE000000000000000000002", "name", section("main"), null),
                tuple("01JDE000000000000000000003", "count", section("main.rows"), section("main.rows")),
                // a plain section inside a row: its values are in that row
                tuple("01JDE000000000000000000004", "note", section("main.rows.details"), section("main.rows")),
                tuple("01JDE000000000000000000005", "sub", section("main.rows.subrows"), section("main.rows.subrows")));
        assertThat(dictionary.fields()).as("a field whose data element is gone has no record")
            .noneMatch(field -> field.name().equals("gone"));
        assertThat(dictionary.fields().get(0)).isEqualTo(new FormDictionary.FieldRecord(FORM,
            "01JFV000000000000000000001", 1, "01JDE000000000000000000001", null, null, "title",
            "{\"en\":\"Title\",\"ar\":\"Unwan\"}", "Text", null, true, 1));
        assertThat(dictionary.fields().get(2)).extracting(FormDictionary.FieldRecord::valueType,
                FormDictionary.FieldRecord::optionSetId, FormDictionary.FieldRecord::mandatory,
                FormDictionary.FieldRecord::sortOrder, FormDictionary.FieldRecord::label)
            .containsExactly("SelectOne", "01JOS000000000000000000001", false, null, null);
        assertThat(dictionary.skipped()).isZero();
        assertThat(dictionary.versionCount()).isEqualTo(1);
    }

    @Test
    void aKeyAVersionGivesTwiceIsKeptOnceTheFirstInStoredOrder() {
        FormVersionDefinition twice = version(FORM, "01JFV000000000000000000002", 2, """
            [{'name': 'main', 'path': 'main'}, {'name': 'main again', 'path': 'main'}]""", """
            [{'id': 'deUidName01', 'name': 'name', 'path': 'main.deUidName01', 'type': 'Text'},
             {'id': 'deUidName01', 'name': 'name_again', 'path': 'main.deUidName01', 'type': 'Integer'},
             {'id': 'deUidTitle1', 'name': 'title', 'path': 'deUidTitle1', 'type': 'Text'},
             {'id': 'deUidTitle1', 'name': 'title_again', 'path': 'deUidTitle1', 'type': 'Text'},
             {'id': 'deUidName01', 'name': 'name_at_the_top', 'path': 'deUidName01', 'type': 'Text'}]""");

        FormDictionary dictionary = FormDictionary.of(List.of(FormDefinition.of(FORM, List.of(twice))));

        assertThat(dictionary.sections()).extracting(FormDictionary.SectionRecord::name).containsExactly("main");
        assertThat(dictionary.fields()).extracting(FormDictionary.FieldRecord::name, FormDictionary.FieldRecord::sectionId)
            .as("one data element in two places is two placements; twice in one place is one")
            .containsExactly(tuple("name", section("main")), tuple("title", null), tuple("name_at_the_top", null));
        assertThat(dictionary.skipped()).isEqualTo(3);
        assertThat(dictionary.versionsWithDuplicates()).containsExactly("01JFV000000000000000000002");
    }

    @Test
    void recordsComeByFormIdThenVersionNumberThenStoredOrder() {
        FormVersionDefinition v2 = version(FORM, "01JFV000000000000000000012", 2, "[]",
            "[{'id': 'deUidTitle1', 'name': 'title', 'path': 'deUidTitle1', 'type': 'Text'}]");
        FormVersionDefinition v1 = version(FORM, "01JFV000000000000000000011", 1, "[]",
            "[{'id': 'deUidTitle1', 'name': 'title', 'path': 'deUidTitle1', 'type': 'Text'}]");
        FormVersionDefinition other = version(OTHER_FORM, "01JFV000000000000000000021", 7, "[]",
            "[{'id': 'deUidName01', 'name': 'name', 'path': 'deUidName01', 'type': 'Text'}]");

        FormDictionary dictionary = FormDictionary.of(List.of(FormDefinition.of(FORM, List.of(v2, v1)),
            FormDefinition.of(OTHER_FORM, List.of(other))));

        assertThat(dictionary.fields())
            .extracting(FormDictionary.FieldRecord::formId, FormDictionary.FieldRecord::formVersionNumber)
            .containsExactly(tuple(OTHER_FORM, 7), tuple(FORM, 1), tuple(FORM, 2));
        assertThat(FormDictionary.of(List.of(FormDefinition.of(OTHER_FORM, List.of(other)),
            FormDefinition.of(FORM, List.of(v1, v2)))).fields()).isEqualTo(dictionary.fields());
        assertThat(dictionary.versionCount()).isEqualTo(3);
        assertThat(FormDictionary.of(List.of()).fields()).isEmpty();
    }
}
