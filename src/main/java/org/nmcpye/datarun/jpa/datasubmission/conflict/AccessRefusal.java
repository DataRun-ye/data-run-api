package org.nmcpye.datarun.jpa.datasubmission.conflict;

/**
 * The access refusals of the upload checks that a server-side move can cause, which the move rule keeps aside (spec,
 * Conflicts, step 3). The upload names them; detection never sees the checks' own codes.
 */
public enum AccessRefusal {
    /** The stored submission is no longer in an assignment and form the uploader may submit. */
    OUT_OF_SCOPE,
    /** The copy's assignment is outside the uploader's team scope. */
    ASSIGNMENT_REFUSED,
    /** The uploader may not submit the copy's form on its assignment. */
    FORM_NOT_PERMITTED
}
