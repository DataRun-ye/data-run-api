package org.nmcpye.datarun.submission.normalized;

/**
 * Why a stored value has no field to be keyed by.
 */
public enum UnmappedReason {
    /** No version of the form defines the value's data path. */
    PATH_NOT_IN_FORM,
    /**
     * The path resolves to a plain section, because no definition at the path can hold the value, and the value is
     * not an object: a text, a number, a boolean or an array.
     */
    SECTION_HOLDS_VALUE,
    /**
     * The path resolves to a repeating section, because no definition at the path can hold the value, and the value
     * is not an array of objects.
     */
    REPEATING_SECTION_HOLDS_NON_ROWS
}
