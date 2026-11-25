package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.Objects;

/**
 * One field's value in a submission or in one of its rows.
 *
 * @param fieldId           the field id: the stored id of the field's data element
 * @param sectionId         the id of the section holding the field at the value's path, or {@code null} outside every
 *                          section; an attribute of the value, which its field id alone keys inside its row
 * @param path              the data path the value is stored at
 * @param value             the stored value, whole: a number stays a number, a multi-select an array. Never JSON
 *                          null (a stored null is no value). An array or object is a copy, never the stored node
 * @param valueType         the declared value type of the definition that resolved the value, or {@code null} when
 *                          that definition declares none the server knows
 * @param issue             why the value does not parse as that type, or {@code null} when it does; the value is
 *                          kept whole either way
 * @param formVersionId     the stored id of the form version whose definition resolved the value
 * @param fromPinnedVersion whether that version is the submission's pinned version; otherwise the path is one the
 *                          pinned version does not define for this shape, resolved through another version
 */
public record FieldValue(
    String fieldId,
    String sectionId,
    String path,
    JsonNode value,
    ValueType valueType,
    ValueIssue issue,
    String formVersionId,
    boolean fromPinnedVersion) {

    public FieldValue {
        Objects.requireNonNull(fieldId, "fieldId");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(formVersionId, "formVersionId");
    }
}
