package org.nmcpye.datarun.submission.normalized;

/**
 * Where a row's id comes from (the feed's {@code key_kind}).
 */
public enum RowKeyKind {
    /** The stored {@code _id} is a ULID, made by the phone; the id is that text. */
    CLIENT,
    /** The stored {@code _id} is another text, such as the 16-character ids of older apps; the id is that text. */
    LEGACY,
    /** The row has no stored {@code _id} (or an empty one); the id is derived ({@link DerivedIds}). */
    DERIVED,
    /**
     * An earlier row of the same submission already has the id this row would get, usually because both carry the
     * same stored {@code _id}; the id is derived from this row's position instead
     * ({@link DerivedIds#duplicateRowKey}), so no two rows of a submission share an id. Flagged
     * {@link NormalizationFlag#DUPLICATE_ID}; the row keeps its stored id in {@link NormalizedRow#storedId()}.
     */
    DUPLICATE_ID
}
