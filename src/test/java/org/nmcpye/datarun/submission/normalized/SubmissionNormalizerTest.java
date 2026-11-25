package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.nmcpye.datarun.submission.normalized.StoredForms.fieldId;
import static org.nmcpye.datarun.submission.normalized.StoredForms.json;
import static org.nmcpye.datarun.submission.normalized.StoredForms.text;
import static org.nmcpye.datarun.submission.normalized.StoredForms.version;

/**
 * Every rule of the normalized submission, on synthetic forms and documents with invented ids and values.
 */
class SubmissionNormalizerTest {

    /** A form with a 24-hex id, as older forms have. */
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
        return SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000002", text(document)),
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
    void aFieldPlacedInTwoSectionsKeepsOneValuePerFieldAndUnmapsTheOther() {
        // the pinned version places one field in two plain sections: consumers key a value by (row, field), so the
        // first in walk order stays, with its section as an attribute, and the other is unmapped and flagged
        NormalizedSubmission normalized = normalize("{'details': {'shared': 'a'}, 'other': {'shared': 'b'}}");

        assertThat(normalized.values())
            .extracting(FieldValue::fieldId, FieldValue::sectionId, v -> v.value().asText())
            .containsExactly(tuple(fieldId("deUidTwice01"), section("details"), "a"));
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path, UnmappedValue::reason, u -> u.value().asText())
            .containsExactly(tuple("other.shared", UnmappedReason.FIELD_ALREADY_HAS_VALUE, "b"));
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.FIELD_ALREADY_HAS_VALUE, 1));
    }

    @Test
    void aSectionRenamedWithBothNamesStoredKeepsThePinnedPlacement() {
        // the review's probe {a: {q: 1}, b: {q: 2}}: section a in version 1 became b in version 2, the pinned one
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .section("a", false).field("deUidQ000001", "a", "q", "Integer").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", "fvUid000002", 2)
            .section("b", false).field("deUidQ000001", "b", "q", "Integer").build(FORM);
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", text("{'a': {'q': 1}, 'b': {'q': 2}}")),
            FormDefinition.of(FORM, List.of(v1, v2)));

        assertThat(normalized.values())
            .extracting(FieldValue::path, FieldValue::sectionId, FieldValue::fromPinnedVersion, v -> v.value().intValue())
            .containsExactly(tuple("b.q", section("b"), true, 2));
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path, UnmappedValue::reason)
            .containsExactly(tuple("a.q", UnmappedReason.FIELD_ALREADY_HAS_VALUE));
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.FIELD_ALREADY_HAS_VALUE, 1));
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
            .extracting(NormalizedRow::id, NormalizedRow::idOrigin, NormalizedRow::index, NormalizedRow::parentRowId,
                NormalizedRow::sectionId, NormalizedRow::sectionPath)
            .containsExactly(
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9W1", RowIdOrigin.STORED, 1, null, section("items"), "items"),
                tuple("01j2q7y0v6m3n4p5r6s7t8v9w2", RowIdOrigin.STORED, 2, null, section("items"), "items"),
                tuple("aB3dE5fG7hJ9kL1m", RowIdOrigin.STORED, 3, null, section("items"), "items"));
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
            text("""
                {'visit': {'households': [
                   {'members': [{'age': 1}]},
                   {'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9WY', 'members': [{'_id': '', 'age': 2}, {'age': 3}]}]}}""")),
            FormDefinition.of(FORM, List.of(version)));

        // expected ids computed by Python's uuid module and by util-linux uuidgen (see DerivedIdsTest)
        String firstHousehold = "ea8b77f1-ebb9-5f13-aa3f-3aeec966b4ca";
        assertThat(normalized.rows())
            .extracting(NormalizedRow::id, NormalizedRow::idOrigin, NormalizedRow::parentRowId, NormalizedRow::index)
            .containsExactly(
                tuple(firstHousehold, RowIdOrigin.DERIVED, null, 1),
                tuple(DerivedIds.derivedRowId(SUBMISSION, "visit.households.members", firstHousehold, 1),
                    RowIdOrigin.DERIVED, firstHousehold, 1),
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9WY", RowIdOrigin.STORED, null, 2),
                tuple(DerivedIds.derivedRowId(SUBMISSION, "visit.households.members", "01J2Q7Y0V6M3N4P5R6S7T8V9WY", 1),
                    RowIdOrigin.DERIVED, "01J2Q7Y0V6M3N4P5R6S7T8V9WY", 1),
                tuple("dd2bccab-dd79-57f4-a8de-07ab2df663f7", RowIdOrigin.DERIVED, "01J2Q7Y0V6M3N4P5R6S7T8V9WY", 2));
        // the same document gives the same derived ids every time
        assertThat(SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000009",
            text("{'visit': {'households': [{'members': [{'age': 1}]}]}}")),
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
    void aRowIdCarriedAgainKeepsTheFirstRowAndGivesLaterOnesTheirPositionsIdFlagged() {
        NormalizedSubmission normalized = normalize("""
            {'items': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 1},
                       {'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 2,
                        'extra': {'parts': [{'qty': 5}]}},
                       {'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 3}]}""");

        String second = DerivedIds.derivedRowId(SUBMISSION, "items", null, 2);
        assertThat(normalized.rows())
            .extracting(NormalizedRow::id, NormalizedRow::idOrigin, NormalizedRow::storedId, NormalizedRow::parentRowId)
            .containsExactly(
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9W1", RowIdOrigin.STORED, "01J2Q7Y0V6M3N4P5R6S7T8V9W1", null),
                tuple(second, RowIdOrigin.DERIVED, "01J2Q7Y0V6M3N4P5R6S7T8V9W1", null),
                // a row nested in a rekeyed row points to its new id, and derives its own id from it
                tuple(DerivedIds.derivedRowId(SUBMISSION, "items.extra.parts", second, 1), RowIdOrigin.DERIVED, null,
                    second),
                tuple(DerivedIds.derivedRowId(SUBMISSION, "items", null, 3), RowIdOrigin.DERIVED,
                    "01J2Q7Y0V6M3N4P5R6S7T8V9W1", null));
        assertThat(normalized.rows()).extracting(r -> r.values().get(0).value().intValue()).containsExactly(1, 2, 5, 3);
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.DUPLICATE_ID, 2));
        assertThat(normalize("""
            {'items': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 1},
                       {'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'count': 2}]}""").rows())
            .extracting(NormalizedRow::id)
            .as("the same document gets the same ids").containsExactly("01J2Q7Y0V6M3N4P5R6S7T8V9W1", second);
    }

    @Test
    void aRowWhoseDerivedIdIsTakenTooGetsTheNextAttempt() {
        // the second row stores, as its _id, the id the third row's position derives; the third repeats the first's
        String third = DerivedIds.derivedRowId(SUBMISSION, "items", null, 3);
        NormalizedSubmission normalized = normalize("{'items': [{'_id': 'aB3dE5fG7hJ9kL1m', 'count': 1},"
            + " {'_id': '" + third + "', 'count': 2}, {'_id': 'aB3dE5fG7hJ9kL1m', 'count': 3}]}");

        assertThat(normalized.rows()).extracting(NormalizedRow::id, NormalizedRow::idOrigin)
            .containsExactly(
                tuple("aB3dE5fG7hJ9kL1m", RowIdOrigin.STORED),
                tuple(third, RowIdOrigin.STORED),
                tuple(DerivedIds.duplicateRowId(SUBMISSION, "items", null, 3, 2), RowIdOrigin.DERIVED));
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.DUPLICATE_ID, 1));
    }

    @Test
    void oneUlidStoredInTwoCasesIsTwoParentsWithTwoSetsOfIds() {
        // ids are text, never read: the two spellings are two rows, and their nested rows derive different ids
        NormalizedSubmission normalized = normalize("""
            {'items': [{'_id': '01J2Q7Y0V6M3N4P5R6S7T8V9W1', 'extra': {'parts': [{'qty': 1}]}},
                       {'_id': '01j2q7y0v6m3n4p5r6s7t8v9w1', 'extra': {'parts': [{'qty': 2}]}}]}""");

        assertThat(normalized.rows()).extracting(NormalizedRow::id, NormalizedRow::idOrigin, NormalizedRow::parentRowId)
            .containsExactly(
                tuple("01J2Q7Y0V6M3N4P5R6S7T8V9W1", RowIdOrigin.STORED, null),
                tuple(DerivedIds.derivedRowId(SUBMISSION, "items.extra.parts", "01J2Q7Y0V6M3N4P5R6S7T8V9W1", 1),
                    RowIdOrigin.DERIVED, "01J2Q7Y0V6M3N4P5R6S7T8V9W1"),
                tuple("01j2q7y0v6m3n4p5r6s7t8v9w1", RowIdOrigin.STORED, null),
                tuple(DerivedIds.derivedRowId(SUBMISSION, "items.extra.parts", "01j2q7y0v6m3n4p5r6s7t8v9w1", 1),
                    RowIdOrigin.DERIVED, "01j2q7y0v6m3n4p5r6s7t8v9w1"));
        assertThat(normalized.flags()).isEmpty();
    }

    @Test
    void aSecondValueForAFieldKeepsThePinnedVersionsAndUnmapsTheOther() {
        // the field was renamed between versions, and the document holds the old name and the new one
        NormalizedSubmission normalized = normalize("{'details': {'old_name': 'a', 'new_name': 'b'}}");

        assertThat(normalized.values()).extracting(FieldValue::path, FieldValue::fromPinnedVersion, FieldValue::fieldId)
            .containsExactly(tuple("details.new_name", true, fieldId("deUidRename")));
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path, UnmappedValue::reason, u -> u.value().asText())
            .containsExactly(tuple("details.old_name", UnmappedReason.FIELD_ALREADY_HAS_VALUE, "a"));
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.FIELD_ALREADY_HAS_VALUE, 1));
    }

    @Test
    void withoutThePinnedVersionsValueTheFirstInWalkOrderStays() {
        // two older names of one field, neither from the pinned version
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidRenamed", null, "alpha", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", "fvUid000002", 2)
            .field("deUidRenamed", null, "beta", "Text").build(FORM);
        FormVersionDefinition v3 = version("01JV0000000000000000000003", "fvUid000003", 3)
            .field("deUidRenamed", null, "gamma", "Text").build(FORM);
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000003",
            text("{'beta': 'b', 'alpha': 'a'}")), FormDefinition.of(FORM, List.of(v1, v2, v3)));

        assertThat(normalized.values()).extracting(FieldValue::path).containsExactly("alpha");
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path, UnmappedValue::reason)
            .containsExactly(tuple("beta", UnmappedReason.FIELD_ALREADY_HAS_VALUE));
    }

    @Test
    void valuesAreKeptWhole_numbersExactAndMultiSelectsArrays() {
        String longText = "x".repeat(20_000);
        NormalizedSubmission normalized = normalize("{'title': '" + longText + "', 'details': {'choices': ['a', 'b'],"
            + " 'more': {'amount': 12345678901234567890.125}}, 'items': [{'count': 3}]}");

        Map<String, JsonNode> byPath = new java.util.HashMap<>();
        normalized.values().forEach(v -> byPath.put(v.path(), v.value()));
        assertThat(byPath.get("title").textValue()).isEqualTo(longText);
        // every digit kept: a default mapper would read 1.2345678901234567E19
        assertThat(byPath.get("details.more.amount").decimalValue()).isEqualByComparingTo("12345678901234567890.125");
        assertThat(StoredJson.write(byPath.get("details.more.amount"))).isEqualTo("12345678901234567890.125");
        assertThat(normalized.rows().get(0).values().get(0).value().isIntegralNumber()).isTrue();
        assertThat(byPath.get("details.choices")).isEqualTo(json("['a', 'b']"));
    }

    @Test
    void eachResultOwnsItsValues() {
        String document = text("{'_status': 'DONE', 'details': {'choices': ['a']}, 'items': [{'count': 1}]}");

        NormalizedSubmission first = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", document), FORM_DEFINITION);
        ((ArrayNode) first.values().get(0).value()).add("changed");
        NormalizedSubmission second = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", document), FORM_DEFINITION);

        assertThat(second.values().get(0).value()).isEqualTo(json("['a']"));
    }

    @Test
    void storedTextThatIsNotJsonIsKeptWholeAsTextAndFlagged() {
        for (String notJson : List.of("{'title': ", "{'title': 'T'} trailing", "{'n': NaN}", "")) {
            String stored = text(notJson);
            NormalizedSubmission normalized = SubmissionNormalizer.normalize(
                new StoredSubmission(SUBMISSION, "fvUid000002", stored), FORM_DEFINITION);

            assertThat(normalized.values()).as(notJson).isEmpty();
            assertThat(normalized.unmapped()).as(notJson)
                .extracting(UnmappedValue::path, UnmappedValue::reason, u -> u.value().textValue())
                .containsExactly(tuple("", UnmappedReason.DOCUMENT_NOT_PARSED, stored));
            assertThat(normalized.flags()).as(notJson)
                .containsExactly(entry(NormalizationFlag.DOCUMENT_NOT_PARSED, 1));
        }
        // numbers Jackson cannot represent, or longer than 1,000 characters in plain form
        for (String number : java.util.stream.Stream.concat(StoredJsonTest.OUT_OF_RANGE.stream(),
            java.util.stream.Stream.of("1e10000", "1e-10000")).toList()) {
            for (String stored : List.of(number, "{\"title\": " + number + "}")) {
                assertThat(SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000002", stored),
                    FORM_DEFINITION).flags()).as(stored).containsExactly(entry(NormalizationFlag.DOCUMENT_NOT_PARSED, 1));
            }
        }
        // deeper than Jackson's limit of 1,000 levels
        String tooDeep = "{\"k\":".repeat(1_001) + "1" + "}".repeat(1_001);
        assertThat(SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000002", tooDeep),
            FORM_DEFINITION).flags()).containsExactly(entry(NormalizationFlag.DOCUMENT_NOT_PARSED, 1));
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
    void aDocumentThatIsNotAnObjectIsUnmappedWholeAndFlagged_andAMissingOneIsFlagged() {
        NormalizedSubmission array = normalize("['a']");
        assertThat(array.unmapped())
            .extracting(UnmappedValue::path, UnmappedValue::reason, u -> u.value().toString())
            .containsExactly(tuple("", UnmappedReason.DOCUMENT_NOT_AN_OBJECT, "[\"a\"]"));
        assertThat(array.flags()).containsExactly(entry(NormalizationFlag.DOCUMENT_NOT_AN_OBJECT, 1));
        NormalizedSubmission missing = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", null), FORM_DEFINITION);
        assertThat(missing.values()).isEmpty();
        assertThat(missing.unmapped()).isEmpty();
        assertThat(missing.rows()).isEmpty();
        assertThat(missing.flags()).containsExactly(entry(NormalizationFlag.DOCUMENT_MISSING, 1));
    }

    @Test
    void aSubmissionWhosePinnedVersionIsGoneResolvesThroughTheFormsOtherVersions() {
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUidGone00",
            text("{'title': 'T', 'details': {'old_note': 'n', 'nowhere': 1}, 'items': [{'count': 2}]}")),
            FORM_DEFINITION);

        assertThat(normalized.formId()).isEqualTo(FORM);
        assertThat(normalized.formVersionId()).isNull();
        assertThat(normalized.values())
            .extracting(FieldValue::path, FieldValue::formVersionId, FieldValue::fromPinnedVersion)
            .containsExactly(tuple("details.old_note", V1.id(), false), tuple("title", V2.id(), false));
        assertThat(normalized.rows()).extracting(r -> r.values().get(0).formVersionId()).containsExactly(V2.id());
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path, UnmappedValue::reason)
            .containsExactly(tuple("details.nowhere", UnmappedReason.PATH_NOT_IN_FORM));
        assertThat(normalized.flags()).containsOnly(
            entry(NormalizationFlag.PINNED_VERSION_MISSING, 1), entry(NormalizationFlag.PATH_NOT_IN_FORM, 1));
        assertThat(SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, null, text("{'title': 'T'}")),
            FORM_DEFINITION).flagged(NormalizationFlag.PINNED_VERSION_MISSING)).isEqualTo(1);
    }

    @Test
    void withoutAStoredDefinitionOfTheFormEveryValueIsUnmappedAndFlagged() {
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000002",
            text("{'title': 'T', 'details': {'visit_day': '2026-12-03'}, 'items': [{'_id': 'x', 'count': 2}]}")), null);

        assertThat(normalized.formId()).isNull();
        assertThat(normalized.formVersionId()).isNull();
        assertThat(normalized.values()).isEmpty();
        assertThat(normalized.rows()).isEmpty();
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path, UnmappedValue::reason)
            .containsExactly(
                tuple("details.visit_day", UnmappedReason.PATH_NOT_IN_FORM),
                tuple("items", UnmappedReason.PATH_NOT_IN_FORM),
                tuple("title", UnmappedReason.PATH_NOT_IN_FORM));
        assertThat(normalized.flags()).containsOnly(entry(NormalizationFlag.FORM_MISSING, 1),
            entry(NormalizationFlag.PINNED_VERSION_MISSING, 1), entry(NormalizationFlag.PATH_NOT_IN_FORM, 3));
    }

    @Test
    void aValueOfAFieldWhoseDataElementIsGoneIsUnmappedAndFlagged() {
        FormVersionDefinition version = FormVersionDefinition.parse(FORM, "01JV0000000000000000000007", "fvUid000007",
            7, json("[]"), json("[{'id': 'deUidGone001', 'name': 'gone', 'path': 'deUidGone001', 'type': 'Text'}]"),
            uid -> null);
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000007", text("{'gone': 'kept'}")),
            FormDefinition.of(FORM, List.of(version)));

        assertThat(normalized.values()).isEmpty();
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path, UnmappedValue::reason)
            .containsExactly(tuple("gone", UnmappedReason.FIELD_WITHOUT_ID));
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.FIELD_WITHOUT_ID, 1));
    }

    @Test
    void nestingDeeperThanTheWalkGoesIsKeptWholeAndFlagged() {
        // 1,000 levels of objects, the most Jackson parses: the root, "ghost", then 998 levels of "k"
        String document = "{\"ghost\":" + "{\"k\":".repeat(998) + "{\"leaf\":1}" + "}".repeat(998) + "}";

        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", document), FORM_DEFINITION);

        assertThat(normalized.unmapped()).hasSize(1);
        UnmappedValue deep = normalized.unmapped().get(0);
        assertThat(deep.reason()).isEqualTo(UnmappedReason.NESTED_TOO_DEEP);
        assertThat(deep.path().split("\\.")).hasSize(SubmissionNormalizer.MAX_DEPTH + 1);
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.NESTED_TOO_DEEP, 1));
    }

    @Test
    void aValueAtAPathReusedForAnotherFieldIsResolvedAsUsualAndFlagged() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidReuse01", null, "reused", "Text").field("deUidSame001", null, "same", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", "fvUid000002", 2)
            .field("deUidReuse02", null, "reused", "Text").field("deUidSame001", null, "same", "Text").build(FORM);
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission(SUBMISSION, "fvUid000002", text("{'reused': 'r', 'same': 's'}")),
            FormDefinition.of(FORM, List.of(v1, v2)));

        assertThat(normalized.values()).extracting(FieldValue::path, FieldValue::fieldId)
            .containsExactly(tuple("reused", fieldId("deUidReuse02")), tuple("same", fieldId("deUidSame001")));
        assertThat(normalized.flags()).containsExactly(entry(NormalizationFlag.AMBIGUOUS_PATH, 1));
    }

    @Test
    void aDocumentAsDeepAsJacksonParsesNeedsLittleStack() throws Exception {
        // a field holding 998 nested objects, kept whole: parsing is iterative, the walk stops at MAX_DEPTH and no
        // value is copied, so a 256 KB stack is enough
        FormVersionDefinition version = version("01JV0000000000000000000005", "fvUid000005", 5)
            .field("deUidDeep001", null, "deep", "Text").build(FORM);
        String document = "{\"deep\":" + "{\"k\":".repeat(998) + "1" + "}".repeat(998) + "}";
        AtomicReference<Object> outcome = new AtomicReference<>();
        Thread small = new Thread(null, () -> {
            try {
                outcome.set(SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, "fvUid000005", document),
                    FormDefinition.of(FORM, List.of(version))));
            } catch (Throwable failure) {
                outcome.set(failure);
            }
        }, "small-stack", 256 * 1024);
        small.start();
        small.join();

        assertThat(outcome.get()).isInstanceOf(NormalizedSubmission.class);
        NormalizedSubmission normalized = (NormalizedSubmission) outcome.get();
        assertThat(normalized.values()).extracting(FieldValue::path, FieldValue::issue)
            .containsExactly(tuple("deep", ValueIssue.NOT_TEXT));
    }

    @Test
    void flagsCountWhatTheDataHeld() {
        NormalizedSubmission normalized = normalize("""
            {'loose': 7, 'ghost': {'a': 1, 'b': 2}, 'other': [], 'items': {'count': 1},
             'details': {'visit_day': 'not a date', 'choices': 'not a list', 'more': {'amount': 2}}}""");

        assertThat(normalized.flags()).containsOnly(
            entry(NormalizationFlag.PATH_NOT_IN_FORM, 3),
            entry(NormalizationFlag.SECTION_HOLDS_VALUE, 1),
            entry(NormalizationFlag.REPEATING_SECTION_HOLDS_NON_ROWS, 1),
            entry(NormalizationFlag.VALUE_ISSUE, 2));
        assertThat(normalized.flagged(NormalizationFlag.FORM_MISSING)).isZero();
        assertThat(normalize("{'title': 'T'}").flags()).isEmpty();
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
