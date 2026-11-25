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
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
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
                DerivedIds.sectionId(FORM, "visit.households"), ValueType.IntegerZeroOrPositive),
            new FieldDefinition(fieldId("deUidVdate0"), "visit_date", null, ValueType.Date));
    }

    @Test
    void aFieldWhoseDataElementIsGoneIsKeptWithoutAnId() {
        FormVersionDefinition v = FormVersionDefinition.parse(FORM, "01JV0000000000000000000001", "fvUid000001", 1,
            StoredForms.json("[]"),
            StoredForms.json("[{'id': 'deUidNoId00', 'name': 'x', 'path': 'deUidNoId00', 'type': 'Text'}]"),
            uid -> null);

        assertThat(v.fields()).containsExactly(new FieldDefinition(null, "x", null, ValueType.Text));
    }

    @Test
    void malformedStoredDefinitionsAreReadAsFarAsTheyGo() {
        FormVersionDefinition v = FormVersionDefinition.parse(FORM, "01JV0000000000000000000001", "fvUid000001", 1,
            StoredForms.json("{'not': 'an array'}"),
            StoredForms.json("[3, {'name': 'no id'}, {'id': 'deUidNoName'}, null]"),
            uid -> "01JD0000000000000000000001");

        assertThat(v.sections()).isEmpty();
        assertThat(v.fields()).isEmpty();
    }

    @Test
    void withoutAPinnedVersionAPathResolvesThroughTheNewestVersionThatCanHoldIt() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .section("stock", false).field("deUidCount1", null, "count", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", "fvUid000002", 2)
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
        FormVersionDefinition v = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidOdd000", null, "odd", "NotAType")
            .build(FORM);

        assertThat(v.fields().get(0).valueType()).isNull();
    }

    @Test
    void thePinnedVersionsDefinitionComesFirst() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidCount1", null, "count", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", "fvUid000002", 2)
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
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidOld001", null, "old_question", "Text").build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", "fvUid000002", 2)
            .field("deUidOld002", null, "old_question", "Text").build(FORM);
        FormVersionDefinition v3 = version("01JV0000000000000000000003", "fvUid000003", 3)
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
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .section("stock", false).build(FORM);
        FormVersionDefinition v2 = version("01JV0000000000000000000002", "fvUid000002", 2)
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
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidMulti1", null, "choices", "SelectMulti").build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1));

        for (StoredShape shape : StoredShape.values()) {
            assertThat(form.resolve(v1, "choices", shape).isField()).as(shape.name()).isTrue();
        }
    }

    @Test
    void aStoredSubmissionNamesItsVersionByUid() {
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1).build(FORM);
        FormDefinition form = FormDefinition.of(FORM, List.of(v1));

        assertThat(form.versionByUid("fvUid000001")).containsSame(v1);
        assertThat(form.versionByUid("fvUidOther0")).isEmpty();
        assertThat(form.versionByUid(null)).isEmpty();
        assertThatThrownBy(() -> FormDefinition.of(FORM, List.of(v1,
            version("01JV0000000000000000000002", "fvUid000002", 1).build(FORM))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(form.id()).isEqualTo(FORM);
    }

    @Test
    void storedShapesFollowTheSpike() {
        assertThat(StoredShape.of(StoredForms.json("{}"))).isEqualTo(StoredShape.OBJECT);
        assertThat(StoredShape.of(StoredForms.json("[{}, {'a': 1}]"))).isEqualTo(StoredShape.ROWS);
        assertThat(StoredShape.of(StoredForms.json("[{}, 1]"))).isEqualTo(StoredShape.LEAF);
        assertThat(StoredShape.of(StoredForms.json("['a', 'b']"))).isEqualTo(StoredShape.LEAF);
        assertThat(StoredShape.of(StoredForms.json("[]"))).isEqualTo(StoredShape.ANY);
        assertThat(StoredShape.of(StoredForms.json("3"))).isEqualTo(StoredShape.LEAF);
        assertThat(StoredShape.of(StoredForms.json("'text'"))).isEqualTo(StoredShape.LEAF);
    }
}
