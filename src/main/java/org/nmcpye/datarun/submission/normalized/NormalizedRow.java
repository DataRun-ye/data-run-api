package org.nmcpye.datarun.submission.normalized;

import java.util.List;
import java.util.Objects;

/**
 * One row of a repeating section, keyed by its submission's id and its own id.
 *
 * @param id          the row's id: its stored {@code _id} exactly as stored (a ULID or an older app's id), or a
 *                    derived id when it has none ({@link RowKeyKind})
 * @param keyKind     where the id comes from
 * @param parentRowId the id of the row it is nested in, or {@code null} when its section is outside every row
 * @param sectionId   the id of its repeating section
 * @param sectionPath the data path of its repeating section
 * @param index       its 1-based position among the rows of the same array
 * @param values      its own values, by field id; a nested row's values are on the nested row
 * @param unmapped    its own values that no field keys
 */
public record NormalizedRow(
    String id,
    RowKeyKind keyKind,
    String parentRowId,
    String sectionId,
    String sectionPath,
    int index,
    List<FieldValue> values,
    List<UnmappedValue> unmapped) {

    public NormalizedRow {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(keyKind, "keyKind");
        Objects.requireNonNull(sectionId, "sectionId");
        Objects.requireNonNull(sectionPath, "sectionPath");
        values = List.copyOf(values);
        unmapped = List.copyOf(unmapped);
    }

    /** A data path of one of this row's values, relative to the row: the path after the section's path. */
    public String pathInRow(String path) {
        return path.startsWith(sectionPath + ".") ? path.substring(sectionPath.length() + 1) : path;
    }
}
