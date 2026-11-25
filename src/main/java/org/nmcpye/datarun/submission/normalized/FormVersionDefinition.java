package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One version of a form: its sections and fields, each located by the data path its values are stored at.
 *
 * <p>{@link #parse} reads a version as stored ({@code data_template_version.sections} and {@code .fields}): a
 * section's data path is its stored {@code path}; a field's is its section's path and its
 * {@code name} (its stored {@code path} ends with its data element's uid, which the stored data never uses), or
 * the name alone for a field outside every section.</p>
 *
 * @param id       the form version's stored id
 * @param uid      the form version's uid, by which stored submissions name their pinned version (internal)
 * @param number   the version number; a form's versions have distinct numbers
 * @param sections the version's sections
 * @param fields   the version's fields
 */
public record FormVersionDefinition(
    String id,
    String uid,
    int number,
    List<SectionDefinition> sections,
    List<FieldDefinition> fields) {

    public FormVersionDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(uid, "uid");
        sections = List.copyOf(sections);
        fields = List.copyOf(fields);
    }

    /**
     * Reads one stored form version.
     *
     * @param formId        the form's stored id (section ids derive from it)
     * @param id            the form version's stored id
     * @param uid           the form version's uid
     * @param number        the version number
     * @param sections      the stored {@code sections} array
     * @param fields        the stored {@code fields} array
     * @param fieldIdForUid the stored id of the data element with a given uid, or {@code null} when there is none;
     *                      each field's {@code id} is a data element uid. A field whose data element has no id is
     *                      kept without an id, and its values are then unmapped, never refused
     */
    public static FormVersionDefinition parse(
        String formId,
        String id,
        String uid,
        int number,
        JsonNode sections,
        JsonNode fields,
        Function<String, String> fieldIdForUid) {
        List<SectionDefinition> sectionList = new ArrayList<>();
        if (sections != null) {
            for (JsonNode section : sections) {
                String path = text(section.get("path"));
                if (path == null) {
                    continue;
                }
                JsonNode repeatable = section.get("repeatable");
                sectionList.add(new SectionDefinition(
                    path,
                    repeatable != null && repeatable.asBoolean(false),
                    DerivedIds.sectionId(formId, path)));
            }
        }
        List<FieldDefinition> fieldList = new ArrayList<>();
        if (fields != null) {
            for (JsonNode field : fields) {
                String dataElementUid = text(field.get("id"));
                String name = text(field.get("name"));
                if (dataElementUid == null || name == null) {
                    continue;
                }
                String fieldId = fieldIdForUid.apply(dataElementUid);
                String storedPath = text(field.get("path"));
                String sectionPath = storedPath == null || storedPath.equals(dataElementUid) || storedPath.indexOf('.') < 0
                    ? null
                    : parentPath(storedPath);
                fieldList.add(new FieldDefinition(
                    fieldId,
                    sectionPath == null ? name : sectionPath + "." + name,
                    sectionPath == null ? null : DerivedIds.sectionId(formId, sectionPath),
                    valueType(text(field.get("type")))));
            }
        }
        return new FormVersionDefinition(id, uid, number, sectionList, fieldList);
    }

    /** The path without its last dot-separated key; empty when it has one key. */
    static String parentPath(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(0, dot);
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static ValueType valueType(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            return ValueType.valueOf(stored);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
