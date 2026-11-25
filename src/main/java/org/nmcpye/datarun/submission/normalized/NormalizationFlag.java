package org.nmcpye.datarun.submission.normalized;

/**
 * A condition in a stored submission that the normalizer met and handled without failing, counted per submission in
 * {@link NormalizedSubmission#flags()}. The feed publisher and submission review expose the counts (freshness, an
 * integrity check) instead of failing on the data.
 */
public enum NormalizationFlag {
    /** No definition of the submission's form is stored: every value is unmapped. Counted once. */
    FORM_MISSING,
    /**
     * The pinned form version is not among the form's stored versions: values resolve through the other versions,
     * newest first, and the result names no pinned version. Counted once.
     */
    PINNED_VERSION_MISSING,
    /** Values unmapped because no version defines their path. */
    PATH_NOT_IN_FORM,
    /** Values unmapped because their path is a plain section. */
    SECTION_HOLDS_VALUE,
    /** Values unmapped because their path is a repeating section and they are not rows. */
    REPEATING_SECTION_HOLDS_NON_ROWS,
    /** Values unmapped because their field's data element no longer exists. */
    FIELD_WITHOUT_ID,
    /** Values unmapped because their row (or the submission) already has a value for the same field. */
    FIELD_ALREADY_HAS_VALUE,
    /** Values unmapped because they lie deeper than the walk goes. */
    NESTED_TOO_DEEP,
    /** The stored document is not a JSON object. Counted once. */
    DOCUMENT_NOT_AN_OBJECT,
    /** Values kept whole that do not parse as their field's value type ({@link FieldValue#issue()}). */
    VALUE_ISSUE,
    /** Rows whose id was already taken by an earlier row of the submission, given a derived id instead. */
    DUPLICATE_ID;

    /** The flag that counts values unmapped for this reason. */
    static NormalizationFlag of(UnmappedReason reason) {
        return valueOf(reason.name());
    }
}
