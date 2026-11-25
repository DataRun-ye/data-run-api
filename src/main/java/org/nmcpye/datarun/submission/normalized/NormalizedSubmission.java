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
 * <p><b>Keys are unique</b>, whatever the document holds: no two rows share an id within the submission, and no two
 * values share a field id within one row, or outside every row, whatever their sections. A later row carrying a
 * taken id gets a derived one and keeps its stored id ({@link NormalizationFlag#DUPLICATE_ID}); a second value for a
 * field is unmapped ({@link UnmappedReason#FIELD_ALREADY_HAS_VALUE}). Both are flagged.</p>
 *
 * <p>Lists are in a fixed order, so equal documents give equal normalized submissions whatever their key order:
 * values and unmapped values in walk order (depth first through the plain sections, each object's keys in
 * {@link String#compareTo} order), a value that loses its key moving to the unmapped values; rows depth first, each
 * followed by its nested rows, arrays in stored order.</p>
 *
 * @param submissionId       the submission's stored id
 * @param formId             the form's stored id, or {@code null} when no definition of the form is stored
 *                           ({@link NormalizationFlag#FORM_MISSING})
 * @param formVersionId      the pinned form version's stored id, or {@code null} when it is not among the form's
 *                           stored versions ({@link NormalizationFlag#PINNED_VERSION_MISSING})
 * @param values             the values outside every row
 * @param unmapped           the values outside every row that no field keys
 * @param rows               every row, nested rows included
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
    Map<NormalizationFlag, Integer> flags) {

    public NormalizedSubmission {
        Objects.requireNonNull(submissionId, "submissionId");
        values = List.copyOf(values);
        unmapped = List.copyOf(unmapped);
        rows = List.copyOf(rows);
        flags = flags.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(flags));
    }

    /** How many times the data held this condition; 0 when it did not. */
    public int flagged(NormalizationFlag flag) {
        return flags.getOrDefault(flag, 0);
    }
}
