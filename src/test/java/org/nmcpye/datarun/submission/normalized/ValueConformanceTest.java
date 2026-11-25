package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.normalized.StoredForms.json;
import static org.nmcpye.datarun.submission.normalized.StoredForms.version;

/**
 * A stored value that does not parse as its declared type is kept whole and flagged. The accepted forms are the ones
 * the staging clone stores (invented values here).
 */
class ValueConformanceTest {

    private static ValueIssue check(ValueType type, String jsonValue) {
        return ValueConformance.check(type, json("{'v': " + jsonValue + "}").get("v"));
    }

    @Test
    void textsCodesAndIdsAreStrings() {
        for (ValueType type : List.of(ValueType.Text, ValueType.LongText, ValueType.FullName, ValueType.SelectOne,
            ValueType.Team, ValueType.OrganisationUnit, ValueType.Reference, ValueType.Progress, ValueType.ScannedCode)) {
            assertThat(check(type, "'any text'")).as(type.name()).isNull();
            assertThat(check(type, "''")).as(type.name()).isNull();
            assertThat(check(type, "12")).as(type.name()).isEqualTo(ValueIssue.NOT_TEXT);
            assertThat(check(type, "['a']")).as(type.name()).isEqualTo(ValueIssue.NOT_TEXT);
        }
    }

    @Test
    void numbersAreJsonNumbers() {
        assertThat(check(ValueType.Number, "2.75")).isNull();
        assertThat(check(ValueType.Number, "-3")).isNull();
        assertThat(check(ValueType.Number, "'2.75'")).isEqualTo(ValueIssue.NOT_A_NUMBER);
        assertThat(check(ValueType.Percentage, "140")).isNull();
        assertThat(check(ValueType.UnitInterval, "true")).isEqualTo(ValueIssue.NOT_A_NUMBER);
    }

    @Test
    void integersAreWholeNumbersOfTheirSign() {
        assertThat(check(ValueType.Integer, "-4")).isNull();
        assertThat(check(ValueType.Integer, "4.0")).isNull();
        assertThat(check(ValueType.Integer, "4.5")).isEqualTo(ValueIssue.NOT_AN_INTEGER);
        assertThat(check(ValueType.Integer, "'4'")).isEqualTo(ValueIssue.NOT_AN_INTEGER);
        assertThat(check(ValueType.IntegerPositive, "1")).isNull();
        assertThat(check(ValueType.IntegerPositive, "0")).isEqualTo(ValueIssue.OUT_OF_RANGE);
        assertThat(check(ValueType.IntegerZeroOrPositive, "0")).isNull();
        assertThat(check(ValueType.IntegerZeroOrPositive, "-1")).isEqualTo(ValueIssue.OUT_OF_RANGE);
        assertThat(check(ValueType.IntegerNegative, "-1")).isNull();
        assertThat(check(ValueType.IntegerNegative, "0")).isEqualTo(ValueIssue.OUT_OF_RANGE);
    }

    @Test
    void booleansAreJsonBooleans() {
        assertThat(check(ValueType.Boolean, "false")).isNull();
        assertThat(check(ValueType.YesNo, "true")).isNull();
        assertThat(check(ValueType.YesNo, "'Yes'")).isEqualTo(ValueIssue.NOT_A_BOOLEAN);
        assertThat(check(ValueType.TrueOnly, "true")).isNull();
        assertThat(check(ValueType.TrueOnly, "false")).isEqualTo(ValueIssue.NOT_A_BOOLEAN);
    }

    @Test
    void datesAreCalendarDatesAloneOrWithATime() {
        assertThat(check(ValueType.Date, "'2026-12-03'")).isNull();
        assertThat(check(ValueType.Date, "'2026-12-03T00:00:00.000'")).isNull();
        assertThat(check(ValueType.Age, "'1990-05-01T00:00:00.000Z'")).isNull();
        assertThat(check(ValueType.Date, "'2026-02-30'")).isEqualTo(ValueIssue.NOT_A_DATE);
        assertThat(check(ValueType.Date, "'03/12/2026'")).isEqualTo(ValueIssue.NOT_A_DATE);
        assertThat(check(ValueType.Date, "'2026-12-03T25:00'")).isEqualTo(ValueIssue.NOT_A_DATE);
        assertThat(check(ValueType.Date, "20261203")).isEqualTo(ValueIssue.NOT_A_DATE);
        assertThat(check(ValueType.Age, "''")).isEqualTo(ValueIssue.NOT_A_DATE);
    }

    @Test
    void dateTimesNeedATimeAfterATOrASpace() {
        assertThat(check(ValueType.DateTime, "'2026-12-03 10:15:30'")).isNull();
        assertThat(check(ValueType.DateTime, "'2026-12-03T10:15:30.123+03:00'")).isNull();
        assertThat(check(ValueType.DateTime, "'2026-12-03T10:15Z'")).isNull();
        assertThat(check(ValueType.DateTime, "'2026-12-03'")).isEqualTo(ValueIssue.NOT_A_DATE_TIME);
        assertThat(check(ValueType.DateTime, "'2026-12-03 10'")).isEqualTo(ValueIssue.NOT_A_DATE_TIME);
    }

    @Test
    void timesAreTimesOfDay() {
        assertThat(check(ValueType.Time, "'08:30'")).isNull();
        assertThat(check(ValueType.Time, "'08:30:15'")).isNull();
        assertThat(check(ValueType.Time, "'24:00'")).isEqualTo(ValueIssue.NOT_A_TIME);
        assertThat(check(ValueType.Time, "830")).isEqualTo(ValueIssue.NOT_A_TIME);
    }

    @Test
    void multiSelectsAreArraysOfStrings() {
        assertThat(check(ValueType.SelectMulti, "['a', 'b']")).isNull();
        assertThat(check(ValueType.SelectMulti, "[]")).isNull();
        assertThat(check(ValueType.SelectMulti, "'a'")).isEqualTo(ValueIssue.NOT_A_LIST_OF_CODES);
        assertThat(check(ValueType.SelectMulti, "['a', 1]")).isEqualTo(ValueIssue.NOT_A_LIST_OF_CODES);
    }

    @Test
    void aFloatingNumberThatIsNotFiniteIsNoNumber() {
        // never parsed from JSON, but a node built in code can hold one, and the check must not fail on it
        assertThat(ValueConformance.check(ValueType.Number, DoubleNode.valueOf(Double.NaN)))
            .isEqualTo(ValueIssue.NOT_A_NUMBER);
        assertThat(ValueConformance.check(ValueType.IntegerPositive, DoubleNode.valueOf(Double.POSITIVE_INFINITY)))
            .isEqualTo(ValueIssue.NOT_AN_INTEGER);
        assertThat(ValueConformance.check(ValueType.Integer, DoubleNode.valueOf(3.0))).isNull();
    }

    @Test
    void aDecimalBeyondTheRangeOfADoubleIsStillANumber() throws Exception {
        JsonNode huge = StoredJson.parse("1e400");
        assertThat(ValueConformance.check(ValueType.Number, huge)).isNull();
        assertThat(ValueConformance.check(ValueType.Integer, huge)).isNull();
    }

    @Test
    void typesWithoutARuleAreNotJudged() {
        assertThat(check(ValueType.Coordinate, "{'lat': 1}")).isNull();
        assertThat(check(null, "{'x': 1}")).isNull();
    }

    @Test
    void theNormalizerKeepsAValueThatDoesNotParseWholeAndFlagsIt() {
        String form = "01HZY8X3K2ABCDEFGHJKMNPQRS";
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidCount01", null, "count", "IntegerPositive")
            .field("deUidDay0001", null, "day", "Date")
            .build(form);
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission("01J2Q7Y0V6M3N4P5R6S7T8V9WX", "fvUid000001",
                StoredForms.text("{'count': '7 people', 'day': '2026-12-03'}")),
            FormDefinition.of(form, List.of(v1)));

        assertThat(normalized.values()).extracting(FieldValue::path, FieldValue::issue)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("count", ValueIssue.NOT_AN_INTEGER),
                org.assertj.core.groups.Tuple.tuple("day", null));
        assertThat(normalized.values().get(0).value().textValue()).isEqualTo("7 people");
    }
}
