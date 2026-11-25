package org.nmcpye.datarun.submission.normalized;

/**
 * Why a stored value does not parse as its field's declared value type ({@link ValueConformance} has the rules). The
 * value is kept whole either way; the issue only flags it, for submission review's {@code valueIssues}.
 */
public enum ValueIssue {
    /** A text, code or id type holds something other than a JSON string. */
    NOT_TEXT,
    /** A number type holds something other than a JSON number. */
    NOT_A_NUMBER,
    /** An integer type holds a number with a fractional part, or something other than a JSON number. */
    NOT_AN_INTEGER,
    /** An integer outside its type's sign: a positive integer at or below zero, for example. */
    OUT_OF_RANGE,
    /** A boolean or yes/no type holds something other than a JSON boolean, or a true-only type holds false. */
    NOT_A_BOOLEAN,
    /** A date type holds something other than a calendar date, alone or followed by a time. */
    NOT_A_DATE,
    /** A date-time type holds something other than a date followed by a time. */
    NOT_A_DATE_TIME,
    /** A time type holds something other than a time of day. */
    NOT_A_TIME,
    /** A multi-select holds something other than a JSON array of strings. */
    NOT_A_LIST_OF_CODES
}
