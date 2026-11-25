package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.nmcpye.datarun.submission.normalized.StoredForms.fieldId;
import static org.nmcpye.datarun.submission.normalized.StoredForms.json;
import static org.nmcpye.datarun.submission.normalized.StoredForms.version;

/**
 * Every rule of the normalized submission, on synthetic forms and documents with invented ids and values.
 */
class SubmissionNormalizerTest {

    /** A form with a 24-hex id, as 27 of the 55 forms on the staging clone have. */
    private static final String FORM = "5f2a9c0b1d3e4f5061728394";
    private static final String SUBMISSION = "01J2Q7Y0V6M3N4P5R6S7T8V9WX";

    private static final FormVersionDefinition V1 = version("01JV0000000000000000000001", "fvUid000001", 1)
        .section("details", false)
        .section("items", true)
        .field("deUidOldNote", "details", "old_note", "Text")
        .field("deUidRename", "details", "old_name", "Text")
        .field("deUidCount01", "items", "count", "Integer")
        .build(FORM);

    private static final FormVersionDefinition V2 = version("01JV0000000000000000000002", "fvUid000002", 2)
        .section("details", false)
        .section("details.more", false)
        .section("other", false)
        .section("items", true)
        .section("items.extra", false)
        .section("items.extra.parts", true)
        .field("deUidTitle01", null, "title", "Text")
        .field("deUidDay0001", "details", "visit_day", "Date")
        .field("deUidMulti01", "details", "choices", "SelectMulti")
        .field("deUidAmount1", "details.more", "amount", "Number")
        .field("deUidTwice01", "details", "shared", "Text")
        .field("deUidTwice01", "other", "shared", "Text")
        .field("deUidRename", "details", "new_name", "Text")
        .field("deUidCount01", "items", "count", "Integer")
        .field("deUidKind001", "items.extra", "kind", "SelectOne")
        .field("deUidPartQty", "items.extra.parts", "qty", "Integer")
        .build(FORM);

    private static final FormDefinition FORM_DEFINITION = FormDefinition.of(FORM, List.of(V1, V2));

    private static NormalizedSubmission normalize(String document) {
        return SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000002", json(document)),
            FORM_DEFINITION);
    }

    private static String section(String path) {
        return DerivedIds.sectionId(FORM, path);
    }

    @Test
    void valuesAreKeyedByFieldIdAndResolvedAgainstThePinnedVersion() {
        NormalizedSubmission normalized = normalize(
            "{'title': 'T', 'details': {'visit_day': '2026-12-03', 'more': {'amount': 2.5}}}");

        assertThat(normalized.submissionId()).isEqualTo(SUBMISSION);
        assertThat(normalized.formId()).isEqualTo(FORM);
        assertThat(normalized.formVersionId()).isEqualTo(V2.id());
        assertThat(normalized.values())
            .extracting(FieldValue::fieldId, FieldValue::sectionId, FieldValue::path, FieldValue::valueType,
                FieldValue::formVersionId, FieldValue::fromPinnedVersion)
            .containsExactly(
                tuple(fieldId("deUidAmount1"), section("details.more"), "details.more.amount", ValueType.Number,
                    V2.id(), true),
                tuple(fieldId("deUidDay0001"), section("details"), "details.visit_day", ValueType.Date, V2.id(), true),
                tuple(fieldId("deUidTitle01"), null, "title", ValueType.Text, V2.id(), true));
        assertThat(normalized.values()).extracting(FieldValue::fieldId).doesNotContain("deUidTitle01");
        assertThat(normalized.unmapped()).isEmpty();
        assertThat(normalized.rows()).isEmpty();
    }

    @Test
    void aFieldPlacedInTwoSectionsKeepsBothValuesApartByItsSection() {
        NormalizedSubmission normalized = normalize("{'details': {'shared': 'a'}, 'other': {'shared': 'b'}}");

        assertThat(normalized.values())
            .extracting(FieldValue::fieldId, FieldValue::sectionId, v -> v.value().asText())
            .containsExactly(
                tuple(fieldId("deUidTwice01"), section("details"), "a"),
                tuple(fieldId("deUidTwice01"), section("other"), "b"));
        assertThat(normalized.keysAreUnique()).isTrue();
    }

    @Test
    void aPathThePinnedVersionDoesNotDefineResolvesThroughTheFormsOtherVersions() {
        NormalizedSubmission normalized = normalize("{'details': {'old_note': 'kept'}}");

        FieldValue value = normalized.values().get(0);
        assertThat(value.fieldId()).isEqualTo(fieldId("deUidOldNote"));
        assertThat(value.fromPinnedVersion()).isFalse();
        assertThat(value.formVersionId()).isEqualTo(V1.id());
        assertThat(normalized.formVersionId()).isEqualTo(V2.id());
    }

    @Test
    void valuesNoFieldCanKeyAreUnmappedWholeWithTheirPathAndReason() {
        NormalizedSubmission normalized = normalize("""
            {'loose': 7,
             'ghost': {'inner': 'x', 'deeper': {'leaf': [1, 2]}},
             'ghost_rows': [{'a': 1}, {'b': 2}],
             'details': {'more': 'text where a section is', 'zero': []},
             'other': [],
             'items': {'count': 1}}""");

        assertThat(normalized.values()).isEmpty();
        assertThat(normalized.unmapped())
            .extracting(UnmappedValue::path, UnmappedValue::reason, u -> u.value().toString())
            .containsExactly(
                tuple("details.more", UnmappedReason.SECTION_HOLDS_VALUE, "\"text where a section is\""),
                tuple("details.zero", UnmappedReason.PATH_NOT_IN_FORM, "[]"),
                tuple("ghost.deeper.leaf", UnmappedReason.PATH_NOT_IN_FORM, "[1,2]"),
                tuple("ghost.inner", UnmappedReason.PATH_NOT_IN_FORM, "\"x\""),
                tuple("ghost_rows", UnmappedReason.PATH_NOT_IN_FORM, "[{\"a\":1},{\"b\":2}]"),
                tuple("items", UnmappedReason.REPEATING_SECTION_HOLDS_NON_ROWS, "{\"count\":1}"),
                tuple("loose", UnmappedReason.PATH_NOT_IN_FORM, "7"),
                tuple("other", UnmappedReason.SECTION_HOLDS_VALUE, "[]"));
    }

    @Test
    void nothingStoredIsDroppedOnlyNullsAndStructureKeys() {
        String document = """
            {'_status': 'DONE', 'title': 'T', 'loose': {'a': 1, 'b': [true, null]},
             'details': {'visit_day': null, 'choices': ['x', 'y'], '_status': 'kept'},
             'items': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', '_index': 1, '_parentId': 'p', '_submissionUid': 's',
                        'count': 3, 'stray': 'z', 'extra': {'kind': 'k', 'parts': [{'qty': 1}, {'qty': null}]}}]}""";
        NormalizedSubmission normalized = normalize(document);

        List<JsonNode> out = new ArrayList<>();
        normalized.values().forEach(v -> out.add(v.value()));
        normalized.unmapped().forEach(u -> out.add(u.value()));
        for (NormalizedRow row : normalized.rows()) {
            row.values().forEach(v -> out.add(v.value()));
            row.unmapped().forEach(u -> out.add(u.value()));
        }
        // every stored leaf except the nulls and the structure keys, each exactly once
        assertThat(out).extracting(JsonNode::toString).containsExactlyInAnyOrder(
            "\"T\"", "1", "[true,null]", "[\"x\",\"y\"]", "\"kept\"", "3", "\"z\"", "\"k\"", "1");
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path)
            .containsExactly("details._status", "loose.a", "loose.b");
        assertThat(normalized.rows().get(0).unmapped()).extracting(UnmappedValue::path).containsExactly("items.stray");
        assertThat(normalized.rows().get(0).pathInRow("items.stray")).isEqualTo("stray");
    }

    @Test
    void aRowIsKeyedByItsStoredIdExactlyAsStored() {
        NormalizedSubmission normalized = normalize("""
            {'items': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 1},
                       {'_id': '01j2q7y0v6m3n4p5r6s7t8v9w2', 'count': 2},
                       {'_id': 'aB3dE5fG7hJ9kL1m', 'count': 3}]}""");

        assertThat(normalized.rows())
            .extracting(NormalizedRow::id, NormalizedRow::keyKind, NormalizedRow::index, NormalizedRow::parentRowId,
                NormalizedRow::sectionId, NormalizedRow::sectionPath)
            .containsExactly(
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9W1", RowKeyKind.CLIENT, 1, null, section("items"), "items"),
                tuple("01j2q7y0v6m3n4p5r6s7t8v9w2", RowKeyKind.CLIENT, 2, null, section("items"), "items"),
                tuple("aB3dE5fG7hJ9kL1m", RowKeyKind.LEGACY, 3, null, section("items"), "items"));
        assertThat(normalized.rows()).extracting(r -> r.values().get(0).value().intValue()).containsExactly(1, 2, 3);
        assertThat(normalized.rows().get(0).values().get(0).fieldId()).isEqualTo(fieldId("deUidCount01"));
        assertThat(normalized.values()).isEmpty();
    }

    @Test
    void aRowWithoutAStoredIdGetsTheDerivedIdOfThePublishedRecipe() {
        FormVersionDefinition version = version("01JV0000000000000000000009", "fvUid000009", 9)
            .section("visit", false)
            .section("visit.households", true)
            .section("visit.households.members", true)
            .field("deUidMember", "visit.households.members", "age", "Integer")
            .build(FORM);
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000009",
            json("""
                {'visit': {'households': [
                   {'members': [{'age': 1}]},
                   {'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9WY', 'members': [{'_id': '', 'age': 2}, {'age': 3}]}]}}""")),
            FormDefinition.of(FORM, List.of(version)));

        // expected ids computed by Python's uuid module and by the spike's SQL (see DerivedIdsTest)
        String firstHousehold = "0aac2c77-7aa8-5403-a4ee-1230d1d35147";
        assertThat(normalized.rows())
            .extracting(NormalizedRow::id, NormalizedRow::keyKind, NormalizedRow::parentRowId, NormalizedRow::index)
            .containsExactly(
                tuple(firstHousehold, RowKeyKind.DERIVED, null, 1),
                tuple(DerivedIds.derivedRowKey(DerivedIds.submissionKey(SUBMISSION), "visit.households.members",
                    java.util.UUID.fromString(firstHousehold), 1).toString(), RowKeyKind.DERIVED, firstHousehold, 1),
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9WY", RowKeyKind.CLIENT, null, 2),
                tuple(DerivedIds.derivedRowKey(DerivedIds.submissionKey(SUBMISSION), "visit.households.members",
                    DerivedIds.ulidToUuid("01J2Q7Y0V6M3N4P5R6S7T8V9WY"), 1).toString(),
                    RowKeyKind.DERIVED, "01J2Q7Y0V6M3N4P5R6S7T8V9WY", 1),
                tuple("c79f5631-38cd-5ae1-9790-34e02d09b7aa", RowKeyKind.DERIVED, "01J2Q7Y0V6M3N4P5R6S7T8V9WY", 2));
        // the same document gives the same derived ids every time
        assertThat(SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000009",
            json("{'visit': {'households': [{'members': [{'age': 1}]}]}}")),
            FormDefinition.of(FORM, List.of(version))).rows().get(0).id()).isEqualTo(firstHousehold);
    }

    @Test
    void aNestedRowPointsToTheRowItIsIn_throughPlainSections() {
        NormalizedSubmission normalized = normalize("""
            {'items': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 1,
                        'extra': {'kind': 'k', 'parts': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W2', 'qty': 5}]}},
                       {'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W3', 'parts': [], 'extra': {'parts': []}}]}""");

        assertThat(normalized.rows())
            .extracting(NormalizedRow::id, NormalizedRow::parentRowId, NormalizedRow::sectionPath, NormalizedRow::index)
            .containsExactly(
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9W1", null, "items", 1),
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9W2", "01J2Q7Y0V6M3N4P5R6S7T8V9W1", "items.extra.parts", 1),
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9W3", null, "items", 2));
        NormalizedRow outer = normalized.rows().get(0);
        assertThat(outer.values()).extracting(FieldValue::fieldId, FieldValue::sectionId)
            .containsExactly(tuple(fieldId("deUidCount01"), section("items")),
                tuple(fieldId("deUidKind001"), section("items.extra")));
        assertThat(outer.pathInRow("items.extra.kind")).isEqualTo("extra.kind");
        NormalizedRow nested = normalized.rows().get(1);
        assertThat(nested.sectionId()).isEqualTo(section("items.extra.parts"));
        assertThat(nested.values()).extracting(FieldValue::fieldId).containsExactly(fieldId("deUidPartQty"));
        // 'parts' directly under the row is no section of the form: an empty array there is unmapped, not zero rows
        assertThat(normalized.rows().get(2).unmapped()).extracting(UnmappedValue::path).containsExactly("items.parts");
    }

    @Test
    void aRowIdTwiceInOneSubmissionIsReportedAndBothRowsAreKept() {
        NormalizedSubmission normalized = normalize("""
            {'items': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 1},
                       {'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 2}]}""");

        assertThat(normalized.rows()).hasSize(2);
        assertThat(normalized.duplicateRowIds()).containsExactly("01J2Q7Y0V6M3N4P5R6S7T8V9W1");
        assertThat(normalized.keysAreUnique()).isFalse();
    }

    @Test
    void oneFieldUnderTwoStoredNamesIsReportedAndBothValuesAreKept() {
        // the field was renamed between versions, and the document holds the old name and the new one
        NormalizedSubmission normalized = normalize("{'details': {'old_name': 'a', 'new_name': 'b'}}");

        assertThat(normalized.values()).extracting(FieldValue::path, FieldValue::fromPinnedVersion)
            .containsExactly(tuple("details.new_name", true), tuple("details.old_name", false));
        assertThat(normalized.duplicateValueKeys()).containsExactly(
            new NormalizedSubmission.ValueKey(null, section("details"), fieldId("deUidRename")));
    }

    @Test
    void valuesAreKeptWhole_numbersStayNumbersAndMultiSelectsStayArrays() {
        String longText = "x".repeat(20_000);
        JsonNode document = json("{'title': '" + longText + "', 'details': {'choices': ['a', 'b'],"
            + " 'more': {'amount': 2.75}}, 'items': [{'count': 3}]}");
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", document), FORM_DEFINITION);

        Map<String, JsonNode> byPath = new java.util.HashMap<>();
        normalized.values().forEach(v -> byPath.put(v.path(), v.value()));
        // a text or a number is the stored node itself: never cut, never converted
        assertThat(byPath.get("title")).isSameAs(document.get("title"));
        assertThat(byPath.get("title").textValue()).hasSize(20_000);
        assertThat(byPath.get("details.more.amount")).isSameAs(document.get("details").get("more").get("amount"));
        assertThat(byPath.get("details.more.amount").isNumber()).isTrue();
        assertThat(normalized.rows().get(0).values().get(0).value())
            .isSameAs(document.get("items").get(0).get("count"));
        // an array is an equal copy
        assertThat(byPath.get("details.choices").isArray()).isTrue();
        assertThat(byPath.get("details.choices")).isEqualTo(document.get("details").get("choices"))
            .isNotSameAs(document.get("details").get("choices"));
    }

    @Test
    void theStoredDocumentIsNeverChangedNorShared() {
        JsonNode document = json("{'_status': 'DONE', 'details': {'choices': ['a']}, 'items': [{'count': 1}]}");
        JsonNode before = document.deepCopy();

        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", document), FORM_DEFINITION);
        ((ArrayNode) normalized.values().get(0).value()).add("changed");

        assertThat(document).isEqualTo(before);
        assertThat(document.get("items").get(0).has("_id")).isFalse();
    }

    @Test
    void equalDocumentsGiveEqualResultsWhateverTheirKeyOrder() {
        NormalizedSubmission one = normalize("""
            {'title': 'T', 'details': {'visit_day': '2026-12-03', 'shared': 's', 'loose': 1},
             'items': [{'count': 1, 'extra': {'kind': 'k'}}]}""");
        NormalizedSubmission other = normalize("""
            {'items': [{'extra': {'kind': 'k'}, 'count': 1}],
             'details': {'loose': 1, 'shared': 's', 'visit_day': '2026-12-03'}, 'title': 'T'}""");

        assertThat(other).isEqualTo(one);
        assertThat(normalize("{'title': 'T'}")).isEqualTo(normalize("{'title': 'T'}"));
    }

    @Test
    void aDocumentThatIsNotAnObjectIsUnmappedWholeAndAMissingOneIsEmpty() {
        assertThat(normalize("['a']").unmapped())
            .extracting(UnmappedValue::path, UnmappedValue::reason)
            .containsExactly(tuple("", UnmappedReason.PATH_NOT_IN_FORM));
        NormalizedSubmission missing = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", null), FORM_DEFINITION);
        assertThat(missing.values()).isEmpty();
        assertThat(missing.unmapped()).isEmpty();
        assertThat(missing.rows()).isEmpty();
    }

    @Test
    void aSubmissionPinnedToAVersionTheFormDoesNotHaveIsRefused() {
        assertThatThrownBy(() -> SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUidOther0", json("{}")), FORM_DEFINITION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(SUBMISSION);
    }

    @Test
    void theOutputCarriesNoUid() {
        NormalizedSubmission normalized = normalize("""
            {'title': 'T', 'details': {'visit_day': '2026-12-03'}, 'items': [{'count': 1}]}""");

        List<String> ids = new ArrayList<>(List.of(normalized.formId(), normalized.formVersionId()));
        normalized.values().forEach(v -> ids.add(v.fieldId()));
        normalized.rows().forEach(r -> {
            ids.add(r.id());
            ids.add(r.sectionId());
            r.values().forEach(v -> ids.add(v.fieldId()));
        });
        // an 11-character uid is never an id of the normalized submission
        assertThat(ids).allSatisfy(id -> assertThat(id).hasSizeGreaterThan(11));
        for (Iterator<String> it = ids.iterator(); it.hasNext(); ) {
            assertThat(it.next()).doesNotStartWith("deUid").doesNotStartWith("fvUid");
        }
    }
}
