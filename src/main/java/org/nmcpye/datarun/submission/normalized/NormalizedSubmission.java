package org.nmcpye.datarun.submission.normalized;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A stored submission resolved against its pinned form version: values keyed by field id, rows keyed by the
 * submission's id and the row's id, the values no field keys, and the flags of what the data held. It holds no
 * placement, version or deleted flag: each caller reads those from where it reads the document (the journal,
 * {@code data_submission}).
 *
 * <p>Lists are in a fixed order, so equal documents give equal normalized submissions whatever their key order:
 * values and unmapped values by data path, depth first through the plain sections, with each object's keys in
 * {@link String#compareTo} order; rows depth first, each followed by its nested rows, arrays in stored order.</p>
 *
 * @param submissionId       the submission's stored id
 * @param formId             the form's stored id, or {@code null} when no definition of the form is stored
 *                           ({@link NormalizationFlag#FORM_MISSING})
 * @param formVersionId      the pinned form version's stored id, or {@code null} when it is not among the form's
 *                           stored versions ({@link NormalizationFlag#PINNED_VERSION_MISSING})
 * @param values             the values outside every row
 * @param unmapped           the values outside every row that no field keys
 * @param rows               every row, nested rows included
 * @param duplicateRowIds    row ids that more than one row of this submission carries, each once (none on the staging
 *                           clone); such rows are kept, and a caller keyed by row id must decide what to do
 * @param duplicateValueKeys value keys (row, section, field) that more than one value carries (none on the staging
 *                           clone); every such value is kept
 * @param flags              what the data held that the normalizer handled without failing, counted by kind; only
 *                           kinds that occurred, so an empty map means nothing was flagged
 */
public record NormalizedSubmission(
    String submissionId,
    String formId,
    String formVersionId,
    List<FieldValue> values,
    List<UnmappedValue> unmapped,
    List<NormalizedRow> rows,
    List<String> duplicateRowIds,
    List<ValueKey> duplicateValueKeys,
    Map<NormalizationFlag, Integer> flags) {

    public NormalizedSubmission {
        Objects.requireNonNull(submissionId, "submissionId");
        values = List.copyOf(values);
        unmapped = List.copyOf(unmapped);
        rows = List.copyOf(rows);
        duplicateRowIds = List.copyOf(duplicateRowIds);
        duplicateValueKeys = List.copyOf(duplicateValueKeys);
        flags = flags.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(flags));
    }

    /** Whether every row id, and every value key inside each row and outside every row, is unique. */
    public boolean keysAreUnique() {
        return duplicateRowIds.isEmpty() && duplicateValueKeys.isEmpty();
    }

    /** How many times the data held this condition; 0 when it did not. */
    public int flagged(NormalizationFlag flag) {
        return flags.getOrDefault(flag, 0);
    }

    /**
     * What identifies a value inside a submission.
     *
     * @param rowId     the row holding the value, or {@code null} outside every row
     * @param sectionId the section holding the field, or {@code null} outside every section
     * @param fieldId   the field id
     */
    public record ValueKey(String rowId, String sectionId, String fieldId) {
    }
}
