package org.nmcpye.datarun.submission.normalized;

/**
 * Where a row's id comes from (the feed's {@code id_origin}).
 */
public enum RowIdOrigin {
    /** The row's stored {@code _id}, exactly as stored. */
    STORED,
    /**
     * Derived from the row's place in the submission ({@link DerivedIds}): the row has no stored {@code _id}, or an
     * empty one, or an earlier row of the submission already has the id it would get. In that last case
     * {@link NormalizedRow#storedId()} keeps its stored id, and the submission is flagged
     * {@link NormalizationFlag#DUPLICATE_ID}.
     */
    DERIVED
}
