package org.nmcpye.datarun.submission.normalized;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.submission.normalized.StoredForms.fieldId;
import static org.nmcpye.datarun.submission.normalized.StoredForms.version;

/**
 * Stored definitions read into data paths, and a stored value's resolution: a definition that can hold its shape,
 * the pinned version's first, then the newest other version's.
 */
class FormDefinitionTest {

    private static final String FORM = "01HZY8X3K2ABCDEFGHJKMNPQRS";

    @Test
    void aFieldIsLocatedByItsSectionPathAndItsNameNotByItsDataElementUid() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .section("visit", false)
            .section("visit.households", true)
            .field("deUidHHsize", "visit.households", "household_size", "IntegerZeroOrPositive")
            .field("deUidVdate0", null, "visit_date", "Date")
            .build(FORM);

        assertThat(v1.sections()).extracting(SectionDefinition::path, SectionDefinition::repeating)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("visit", false),
                org.assertj.core.groups.Tuple.tuple("visit.households", true));
        assertThat(v1.sections().get(1).id()).isEqualTo(DerivedIds.sectionId(FORM, "visit.households"));
        assertThat(v1.fields()).containsExactly(
            new FieldDefinition(fieldId("deUidHHsize"), "visit.households.household_size",
                DerivedIds.sectionId(FORM, "visit.households"), ValueType.IntegerZeroOrPositive,
                "IntegerZeroOrPositive", "household_size", null, null, null, null),
            new FieldDefinition(fieldId("deUidVdate0"), "visit_date", null, ValueType.Date, "Date", "visit_date",
                null, null, null, null));
    }

    @Test
    void aFieldWhoseDataElementIsGoneIsKeptWithoutAnId() {
        FormVersionDefinition v = FormVersionDefinition.parse(FORM, "01JV0000000000000000000001", 1,
            StoredForms.json("[]"),
            StoredForms.json("[{'id': 'deUidNoId00', 'name': 'x', 'path': 'deUidNoId00', 'type': 'Text'}]"),
            uid -> null, uid -> null);

        assertThat(v.fields())
            .containsExactly(new FieldDefinition(null, "x", null, ValueType.Text, "Text", "x", null, null, null, null));
    }

    @Test
    void malformedStoredDefinitionsAreReadAsFarAsTheyGo() {
        FormVersionDefinition v = FormVersionDefinition.parse(FORM, "01JV0000000000000000000001", 1,
            StoredForms.json("{'not': 'an array'}"),
            StoredForms.json("[3, {'name': 'no id'}, {'id': 'deUidNoName'}, null]"),
            uid -> "01JD0000000000000000000001", uid -> "01JO0000000000000000000001");

        assertThat(v.sections()).isEmpty();
        assertThat(v.fields()).isEmpty();
    }

    /** What the feed's dictionary publishes of each definition is read as stored; a value of another type is none. */
    @Test
    void theDictionaryAttributesAreReadAsStored() {
        FormVersionDefinition v = FormVersionDefinition.parse(FORM, "01JV0000000000000000000001", 1,
            StoredForms.json("""
                [{'name': 'visit', 'path': 'visit', 'order': 1, 'label': {'en': 'Visit', 'ar': 'Ziyara'}},
                 {'name': 'households', 'path': 'visit.households', 'repeatable': true, 'order': 2},
                 {'path': 'unnamed', 'repeatable': 'yes', 'order': 2.5, 'label': null}]"""),
            StoredForms.json("""
                [{'id': 'deUidSize00', 'name': 'size', 'path': 'visit.households.deUidSize00', 'type': 'Integer',
                  'mandatory': true, 'order': 3, 'label': {'en': 'Size'}, 'optionSet': 'osUidSizes1'},
                 {'id': 'deUidNote00', 'name': 'note', 'path': 'deUidNote00', 'type': 'NoSuchType',
                  'mandatory': 'true', 'order': '4', 'label': 'a bare text', 'optionSet': 'osUidGone01'},
                 {'id': 'deUidBare00', 'name': 'bare', 'path': 'deUidBare00'}]"""),
            uid -> "id-" + uid, uid -> uid.equals("osUidSizes1") ? "01JO0000000000000000000001" : null);

        assertThat(v.sections()).containsExactly(
            new SectionDefinition("visit", false, DerivedIds.sectionId(FORM, "visit"), "visit",
                "{\"en\":\"Visit\",\"ar\":\"Ziyara\"}", 1),
            new SectionDefinition("visit.households", true, DerivedIds.sectionId(FORM, "visit.households"),
                "households", null, 2),
            new SectionDefinition("unnamed", false, DerivedIds.sectionId(FORM, "unnamed"), null, null, null));
        assertThat(v.fields()).containsExactly(
            new FieldDefinition("id-deUidSize00", "visit.households.size", DerivedIds.sectionId(FORM, "visit.households"),
                ValueType.Integer, "Integer", "size", "{\"en\":\"Size\"}", "01JO0000000000000000000001", true, 3),
            // a type the server does not know: no value type for the normalizer, the stored text for the dictionary
            new FieldDefinition("id-deUidNote00", "note", null, null, "NoSuchType", "note", "\"a bare text\"", null,
                null, null),
            new FieldDefinition("id-deUidBare00", "bare", null, null, null, "bare", null, null, null, null));
    }

    @Test
    void withoutAPinnedVersionAPathResolvesThroughTheNewestVersionThatCanHoldIt() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .section("stock", false).field("deUidCount1", null, "count", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", 2)
            .section("stock", true).field("deUidCount1", null, "count", "Integer").build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1, v2));

        FormDefinition.Resolution count = form.resolve(null, "count", StoredShape.LEAF);
        assertThat(count.version()).isSameAs(v2);
        assertThat(count.fromPinnedVersion()).isFalse();
        assertThat(form.resolve(null, "stock", StoredShape.OBJECT).version()).isSameAs(v1);
        assertThat(form.resolve(null, "stock", StoredShape.ROWS).version()).isSameAs(v2);
        assertThat(form.resolve(null, "elsewhere", StoredShape.LEAF)).isNull();
        assertThat(FormDefinition.NO_VERSIONS.resolve(null, "count", StoredShape.LEAF)).isNull();
    }

    @Test
    void anUnknownValueTypeIsNoValueType() {
        FormVersionDefinition v = version("01JV0000000000000000000001", 1)
            .field("deUidOdd000", null, "odd", "NotAType")
            .build(FORM);

        assertThat(v.fields().get(0).valueType()).isNull();
    }

    @Test
    void thePinnedVersionsDefinitionComesFirst() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .field("deUidCount1", null, "count", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", 2)
            .field("deUidCount1", null, "count", "Integer").build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v2, v1));

        FormDefinition.Resolution byV1 = form.resolve(v1, "count", StoredShape.LEAF);
        assertThat(byV1.version()).isSameAs(v1);
        assertThat(byV1.fromPinnedVersion()).isTrue();
        assertThat(byV1.field().valueType()).isEqualTo(ValueType.Text);
        assertThat(form.resolve(v2, "count", StoredShape.LEAF).field().valueType()).isEqualTo(ValueType.Integer);
    }

    @Test
    void aPathThePinnedVersionDoesNotDefineResolvesThroughTheNewestVersionThatDoes() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .field("deUidOld001", null, "old_question", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", 2)
            .field("deUidOld002", null, "old_question", "Text").build(FORM);
        FormVersionDefinition v3 = version("01JV0000000000000000000003", 3)
            .field("deUidNew001", null, "new_question", "Text").build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1, v3, v2));

        FormDefinition.Resolution resolution = form.resolve(v3, "old_question", StoredShape.LEAF);
        assertThat(resolution.version()).isSameAs(v2);
        assertThat(resolution.fromPinnedVersion()).isFalse();
        assertThat(resolution.field().id()).isEqualTo(fieldId("deUidOld002"));
        assertThat(form.resolve(v3, "never_defined", StoredShape.LEAF)).isNull();
        assertThat(form.versions()).extracting(FormVersionDefinition::number).containsExactly(3, 2, 1);
    }

    @Test
    void aDefinitionThatCanHoldTheShapeComesBeforeThePinnedVersions() {
        // plain in version 1, repeating in version 2: an object in a submission pinned to version 2 resolves through
        // version 1, and rows through version 2
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .section("stock", false).build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", 2)
            .section("stock", true).build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1, v2));

        FormDefinition.Resolution object = form.resolve(v2, "stock", StoredShape.OBJECT);
        assertThat(object.version()).isSameAs(v1);
        assertThat(object.isPlainSection()).isTrue();
        assertThat(form.resolve(v2, "stock", StoredShape.ROWS).version()).isSameAs(v2);
        assertThat(form.resolve(v1, "stock", StoredShape.ROWS).isRepeatingSection()).isTrue();
        // an empty array fits every definition, so the pinned version's comes first
        assertThat(form.resolve(v1, "stock", StoredShape.ANY).version()).isSameAs(v1);
        // nothing at the path holds text: the pinned version's definition is kept, and the value will be unmapped
        FormDefinition.Resolution leaf = form.resolve(v2, "stock", StoredShape.LEAF);
        assertThat(leaf.version()).isSameAs(v2);
        assertThat(leaf.isRepeatingSection()).isTrue();
    }

    @Test
    void aFieldHoldsAnyShape() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .field("deUidMulti1", null, "choices", "SelectMulti").build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1));

        for (StoredShape shape : StoredShape.values()) {
            assertThat(form.resolve(v1, "choices", shape).isField()).as(shape.name()).isTrue();
        }
    }

    @Test
    void aStoredSubmissionNamesItsVersionByUid() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1).build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1));

        assertThat(form.versionById("01JV0000000000000000000001")).containsSame(v1);
        assertThat(form.versionById("01JVOTHER00000000000000000")).isEmpty();
        assertThat(form.versionById(null)).isEmpty();
        assertThatThrownBy(() -> FormDefinition.of(FORM, List.of(v1,
            version("01JV0000000000000000000002", 1).build(FORM))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(form.id()).isEqualTo(FORM);
    }

    @Test
    void pathsThatNameDifferentThingsAcrossVersionsOrTwiceInOneAreAmbiguous() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .field("deUidReuse01", null, "reused", "Text")
            .section("flip", false)
            .section("stock", false)
            .field("deUidSame001", null, "same", "Text")
            .field("deUidTwiceA1", null, "twice", "Text")
            .field("deUidTwiceB1", null, "twice", "Integer")
            .build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", 2)
            .field("deUidReuse02", null, "reused", "Text")
            .field("deUidFlip001", null, "flip", "Text")
            .section("stock", true)
            .field("deUidSame001", null, "same", "Text")
            .build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1, v2));

        // another field's path, a section become a field, a path twice in one version; plain to repeating is not
        assertThat(form.ambiguousPaths()).containsExactlyInAnyOrder("reused", "flip", "twice");
        // resolution is unchanged: the pinned version's, and twice in one version the first in stored order
        assertThat(form.resolve(v2, "reused", StoredShape.LEAF).field().id()).isEqualTo(fieldId("deUidReuse02"));
        assertThat(form.resolve(v1, "twice", StoredShape.LEAF).field().id()).isEqualTo(fieldId("deUidTwiceA1"));
    }

    @Test
    void aStoredValuesShapeIsWhatItHolds() {
        assertThat(StoredShape.of(StoredForms.json("{}"))).isEqualTo(StoredShape.OBJECT);
        assertThat(StoredShape.of(StoredForms.json("[{}, {'a': 1}]"))).isEqualTo(StoredShape.ROWS);
        assertThat(StoredShape.of(StoredForms.json("[{}, 1]"))).isEqualTo(StoredShape.LEAF);
        assertThat(StoredShape.of(StoredForms.json("['a', 'b']"))).isEqualTo(StoredShape.LEAF);
        assertThat(StoredShape.of(StoredForms.json("[]"))).isEqualTo(StoredShape.ANY);
        assertThat(StoredShape.of(StoredForms.json("3"))).isEqualTo(StoredShape.LEAF);
        assertThat(StoredShape.of(StoredForms.json("'text'"))).isEqualTo(StoredShape.LEAF);
    }
    /** The reverse of resolution: where every version stores a field's values, for search and edits by field id. */
    @Test
    void aFieldsDataPathsAreListedByVersionNewestFirst() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .section("visit", false)
            .field("deUidMoved0", "visit", "moved", "Integer")
            .build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", 2)
            .section("visit", false)
            .section("visit.households", true)
            .field("deUidMoved0", "visit.households", "moved_again", "Integer")
            .field("deUidMoved0", null, "moved", "Integer")
            .fieldWithoutDataElement("deUidGone00", null, "gone", "Text")
            .build(FORM);
        FormVersionDefinition v3 = version("01JV0000000000000000000003", 3)
            .field("deUidOther0", null, "other", "Text")
            .build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1, v3, v2));

        assertThat(form.dataPaths(fieldId("deUidMoved0"))).containsExactly(
            org.assertj.core.api.Assertions.entry("01JV0000000000000000000002",
                List.of("visit.households.moved_again", "moved")),
            org.assertj.core.api.Assertions.entry("01JV0000000000000000000001", List.of("visit.moved")));
        assertThat(form.dataPaths(fieldId("deUidOther0")))
            .containsExactly(org.assertj.core.api.Assertions.entry("01JV0000000000000000000003", List.of("other")));
        assertThat(form.dataPaths("01JD0000000000000000000099")).as("a field no version places").isEmpty();
        assertThat(form.dataPaths(null)).isEmpty();
        assertThatThrownBy(() -> form.dataPaths(fieldId("deUidMoved0")).clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }
    /**
     * The placements that hold a version's values follow resolution: a data path named twice keeps the definition
     * resolution picks, and one field placed twice in one section keeps the placement first in walk order, whatever
     * the stored order. A submission's values agree: they land under the published placements.
     */
    @Test
    void theFieldPlacementsThatHoldValuesAreTheOnesTheNormalizerKeeps() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1)
            .section("visit", false)
            .field("deUidTwice0", "visit", "zeta", "Integer")
            .field("deUidTwice0", "visit", "alpha", "Integer")
            .field("deUidFirst0", "visit", "shared", "Text")
            .field("deUidSecond", "visit", "shared", "Text")
            .field("deUidTwice0", null, "top", "Integer")
            .fieldWithoutDataElement("deUidGone00", null, "gone", "Text")
            .build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1));

        FormDefinition.Placements placements = form.placements(v1);

        assertThat(placements.published()).extracting(FieldDefinition::path)
            .as("in stored order; a field in two sections is two placements")
            .containsExactly("visit.alpha", "visit.shared", "top");
        assertThat(placements.published().get(1).id()).isEqualTo(fieldId("deUidFirst0"));
        assertThat(placements.placedTwice()).extracting(FieldDefinition::path).containsExactly("visit.zeta");
        assertThat(placements.pathTakenByAnother()).extracting(FieldDefinition::id)
            .containsExactly(fieldId("deUidSecond"));

        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(
            "01J2Q7Y0V6M3N4P5R6S7T8V9WX", StoredForms.FORM_ID, "01JV0000000000000000000001",
            StoredForms.text("{'visit': {'zeta': 1, 'alpha': 2, 'shared': 's'}}")), form);
        assertThat(normalized.values()).extracting(FieldValue::path, FieldValue::fieldId).containsExactly(
            org.assertj.core.groups.Tuple.tuple("visit.alpha", fieldId("deUidTwice0")),
            org.assertj.core.groups.Tuple.tuple("visit.shared", fieldId("deUidFirst0")));
        assertThat(normalized.unmapped()).extracting(UnmappedValue::path).containsExactly("visit.zeta");
    }

    @Test
    void walkOrderComparesKeyByKey() {
        assertThat(FormDefinition.WALK_ORDER.compare("visit.headline", "visit-alt")).isNegative();
        assertThat("visit.headline".compareTo("visit-alt")).as("unlike the text as a whole").isPositive();
        assertThat(FormDefinition.WALK_ORDER.compare("a", "a.b")).isNegative();
        assertThat(FormDefinition.WALK_ORDER.compare("a.b", "a.b")).isZero();
        assertThat(FormDefinition.WALK_ORDER.compare("b", "a.z")).isPositive();
    }

    @Test
    void placementsAreAskedOfTheFormsOwnVersions() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", 1).build(FORM);
        FormVersionDefinition other = version("01JV0000000000000000000009", 9).build(FORM);

        assertThatThrownBy(() -> FormDefinition.of(FORM, List.of(v1)).placements(other))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
