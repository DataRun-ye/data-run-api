package org.nmcpye.datarun.submission.normalized;

import java.util.List;
import java.util.Objects;

/**
 * One row of a repeating section, keyed by its submission's id and its own id.
 *
 * @param id          the row's id, unique within its submission: its stored {@code _id} exactly as stored, or a
 *                    derived id when it has none or an earlier row already has it ({@link RowIdOrigin})
 * @param idOrigin    where the id comes from
 * @param storedId    its stored {@code _id} exactly as stored, or {@code null} when it has none; set while the id is
 *                    derived only when an earlier row already had that stored id
 * @param parentRowId the id of the row it is nested in, or {@code null} when its section is outside every row
 * @param sectionId   the id of its repeating section
 * @param sectionPath the data path of its repeating section
 * @param index       its 1-based position among the rows of the same array
 * @param values      its own values, by field id; a nested row's values are on the nested row
 * @param unmapped    its own values that no field keys
 */
public record NormalizedRow(
    String id,
    RowIdOrigin idOrigin,
    String storedId,
    String parentRowId,
    String sectionId,
    String sectionPath,
    int index,
    List<FieldValue> values,
    List<UnmappedValue> unmapped) {

    public NormalizedRow {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(idOrigin, "idOrigin");
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
