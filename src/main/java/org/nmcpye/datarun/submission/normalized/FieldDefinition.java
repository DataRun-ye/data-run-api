package org.nmcpye.datarun.submission.normalized;

import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.Objects;

/**
 * A field of one form version, located by its data path.
 *
 * @param id        the field id: the stored id of the data element the field carries, never its uid
 * @param path      the data path: the parent section's path and the field's name, joined by a dot; the name alone
 *                  for a field outside every section
 * @param sectionId the id of the section that holds the field, or {@code null} outside every section. One form
 *                  version can place a field twice, in two sections (one version on staging places five), so a
 *                  field's identity inside a form version is this pair, not the field id alone
 * @param valueType the declared value type, or {@code null} when the stored type is missing or unknown
 */
public record FieldDefinition(String id, String path, String sectionId, ValueType valueType) {

    public FieldDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(path, "path");
    }
}
