package org.nmcpye.datarun.submission.normalized;

import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.Objects;

/**
 * A field of one form version, located by its data path, with the attributes the feed's dictionary publishes
 * ({@code feed.form_field}).
 *
 * @param id          the field id: the stored id of the data element the field carries, never its uid; {@code null}
 *                    when that data element no longer exists, and then the field's values are unmapped
 *                    ({@link UnmappedReason#FIELD_WITHOUT_ID})
 * @param path        the data path: the parent section's path and the field's name, joined by a dot; the name alone
 *                    for a field outside every section
 * @param sectionId   the id of the section that holds the field, or {@code null} outside every section. One form
 *                    version can place a field in two sections, so a definition is identified by this pair; a stored
 *                    value is keyed by its field id alone, and carries its section as an attribute
 * @param valueType   the declared value type, or {@code null} when the stored type is missing or unknown
 * @param type        the value type as stored, whatever its text, which the dictionary publishes; {@code null} when
 *                    the definition gives none
 * @param name        the field's name in this version, as stored: the last key of its data path
 * @param label       its labels by locale, as stored, in JSON text; {@code null} when it has none
 * @param optionSetId the stored id of the option set it names, or {@code null} when it names none or that option set
 *                    does not exist
 * @param mandatory   whether it is mandatory, as stored; {@code null} when the definition does not say
 * @param order       its place among its siblings, as stored; {@code null} when the definition gives none
 */
public record FieldDefinition(
    String id,
    String path,
    String sectionId,
    ValueType valueType,
    String type,
    String name,
    String label,
    String optionSetId,
    Boolean mandatory,
    Integer order) {

    public FieldDefinition {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(name, "name");
    }
}
