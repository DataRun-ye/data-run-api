package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Whether a stored value parses as its field's declared value type. It only judges; the value is kept whole either
 * way.
 *
 * <p>The rules follow how phones store values: numbers as JSON numbers, booleans and yes/no as JSON booleans, codes, ids and
 * texts as strings, multi-selects as arrays of strings, dates as {@code yyyy-MM-dd} or as an ISO date and time (an
 * {@code Age} with a zone), date-times with a space before the time, times as {@code HH:mm}.</p>
 *
 * <ul>
 *   <li>Texts, codes and ids ({@code Text}, {@code LongText}, {@code Letter}, {@code FullName}, {@code PhoneNumber},
 *       {@code Email}, {@code URL}, {@code Username}, {@code ScannedCode}, {@code SelectOne}, {@code Team},
 *       {@code OrganisationUnit}, {@code Activity}, {@code Entity}, {@code Progress}, {@code Reference},
 *       {@code Image}, {@code FileResource}): a JSON string. Whether a code is one of the field's options is not
 *       judged here.</li>
 *   <li>{@code Number}, {@code Percentage}, {@code UnitInterval}: a JSON number, judged by its exact decimal value,
 *       however large ({@code 1E+400}). No range is judged: DataRun defines none for the last two.</li>
 *   <li>Integers: a JSON number without a fractional part ({@code 3.0} is whole); {@code IntegerPositive} above
 *       zero, {@code IntegerNegative} below, {@code IntegerZeroOrPositive} not below.</li>
 *   <li>{@code Boolean}, {@code YesNo}: a JSON boolean; {@code TrueOnly}: JSON {@code true}.</li>
 *   <li>{@code Date}, {@code Age}: a calendar date, alone or followed by a time; {@code DateTime}: a date and a time;
 *       the time after {@code T} or a space, as {@code HH:mm}, optional seconds and fraction, optional zone.
 *       {@code Time}: a time of day.</li>
 *   <li>{@code SelectMulti}: a JSON array of strings.</li>
 *   <li>Other types ({@code Coordinate}, {@code GeoJson}, {@code Calculated}, sections) and an unknown type: not
 *       judged.</li>
 * </ul>
 *
 * <p><b>NaN and Infinity never reach it.</b> They are not JSON: stored text holding one does not parse
 * ({@link StoredJson}), and the whole document is kept as text, flagged {@link UnmappedReason#DOCUMENT_NOT_PARSED}.
 * The values it judges come from {@link StoredJson#parse}, whose numbers are ints, longs, big integers and big
 * decimals, never binary floats.</p>
 */
final class ValueConformance {

    private static final Pattern DATE_TIME = Pattern.compile(
        "(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d{1,9})?)?)(?:Z|[+-]\\d{2}(?::?\\d{2})?)?)?");
    private static final Pattern TIME = Pattern.compile("(\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.\\d{1,9})?)?");

    private ValueConformance() {
    }

    /** The issue with the value, or {@code null} when it parses as the type or the type is not judged. */
    static ValueIssue check(ValueType type, JsonNode value) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case Text, LongText, Letter, FullName, PhoneNumber, Email, URL, Username, ScannedCode, SelectOne, Team,
                 OrganisationUnit, Activity, Entity, Progress, Reference, Image, FileResource ->
                value.isTextual() ? null : ValueIssue.NOT_TEXT;
            case Number, Percentage, UnitInterval -> value.isNumber() ? null : ValueIssue.NOT_A_NUMBER;
            case Integer -> integer(value, Sign.ANY);
            case IntegerPositive -> integer(value, Sign.POSITIVE);
            case IntegerNegative -> integer(value, Sign.NEGATIVE);
            case IntegerZeroOrPositive -> integer(value, Sign.ZERO_OR_POSITIVE);
            case Boolean, YesNo -> value.isBoolean() ? null : ValueIssue.NOT_A_BOOLEAN;
            case TrueOnly -> value.isBoolean() && value.booleanValue() ? null : ValueIssue.NOT_A_BOOLEAN;
            case Date, Age -> dateTime(value, false) ? null : ValueIssue.NOT_A_DATE;
            case DateTime -> dateTime(value, true) ? null : ValueIssue.NOT_A_DATE_TIME;
            case Time -> value.isTextual() && time(value.textValue()) ? null : ValueIssue.NOT_A_TIME;
            case SelectMulti -> listOfStrings(value) ? null : ValueIssue.NOT_A_LIST_OF_CODES;
            default -> null;
        };
    }

    private enum Sign { ANY, POSITIVE, NEGATIVE, ZERO_OR_POSITIVE }

    private static ValueIssue integer(JsonNode value, Sign sign) {
        if (!value.isNumber()) {
            return ValueIssue.NOT_AN_INTEGER;
        }
        BigDecimal number = value.decimalValue();
        if (number.signum() != 0 && number.stripTrailingZeros().scale() > 0) {
            return ValueIssue.NOT_AN_INTEGER;
        }
        boolean inRange = switch (sign) {
            case ANY -> true;
            case POSITIVE -> number.signum() > 0;
            case NEGATIVE -> number.signum() < 0;
            case ZERO_OR_POSITIVE -> number.signum() >= 0;
        };
        return inRange ? null : ValueIssue.OUT_OF_RANGE;
    }

    private static boolean dateTime(JsonNode value, boolean timeRequired) {
        if (!value.isTextual()) {
            return false;
        }
        Matcher m = DATE_TIME.matcher(value.textValue());
        if (!m.matches() || (timeRequired && m.group(4) == null)) {
            return false;
        }
        try {
            LocalDate.of(java.lang.Integer.parseInt(m.group(1)), java.lang.Integer.parseInt(m.group(2)),
                java.lang.Integer.parseInt(m.group(3)));
        } catch (DateTimeException e) {
            return false;
        }
        return m.group(4) == null || time(m.group(4));
    }

    private static boolean time(String text) {
        Matcher m = TIME.matcher(text);
        if (!m.matches()) {
            return false;
        }
        try {
            LocalTime.of(java.lang.Integer.parseInt(m.group(1)), java.lang.Integer.parseInt(m.group(2)),
                m.group(3) == null ? 0 : java.lang.Integer.parseInt(m.group(3)));
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static boolean listOfStrings(JsonNode value) {
        if (!value.isArray()) {
            return false;
        }
        for (JsonNode element : value) {
            if (!element.isTextual()) {
                return false;
            }
        }
        return true;
    }
}
