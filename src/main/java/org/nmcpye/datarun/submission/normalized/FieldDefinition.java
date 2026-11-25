package org.nmcpye.datarun.submission.normalized;

import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.Objects;

/**
 * A field of one form version, located by its data path.
 *
 * @param id        the field id: the stored id of the data element the field carries, never its uid; {@code null}
 *                  when that data element no longer exists, and then the field's values are unmapped
 *                  ({@link UnmappedReason#FIELD_WITHOUT_ID})
 * @param path      the data path: the parent section's path and the field's name, joined by a dot; the name alone
 *                  for a field outside every section
 * @param sectionId the id of the section that holds the field, or {@code null} outside every section. One form
 *                  version can place a field in two sections, so a definition is identified by this pair; a stored
 *                  value is keyed by its field id alone, and carries its section as an attribute
 * @param valueType the declared value type, or {@code null} when the stored type is missing or unknown
 */
public record FieldDefinition(String id, String path, String sectionId, ValueType valueType) {

    public FieldDefinition {
        Objects.requireNonNull(path, "path");
    }
}
