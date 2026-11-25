package org.nmcpye.datarun.submission.normalized;

/**
 * Why a stored value has no field to be keyed by. Each reason is also a {@link NormalizationFlag} of the same name.
 */
public enum UnmappedReason {
    /** No version of the form defines the value's data path (or no definition of the form is stored at all). */
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
    REPEATING_SECTION_HOLDS_NON_ROWS,
    /** The path resolves to a field whose data element no longer exists, so the field has no id to key it by. */
    FIELD_WITHOUT_ID,
    /**
     * The value lies deeper than the walk goes ({@link SubmissionNormalizer#MAX_DEPTH} nested objects and rows); it
     * is kept whole at the path where the walk stopped.
     */
    NESTED_TOO_DEEP,
    /** The stored document is not a JSON object; it is kept whole at the empty path. */
    DOCUMENT_NOT_AN_OBJECT
}
