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
 * the name alone for a field outside every section. It also reads what the feed's dictionary publishes of each:
 * name, {@code label}, {@code order}, and for a field its {@code type} as stored, {@code mandatory} and the option
 * set its {@code optionSet} uid names.</p>
 *
 * @param id       the form version's stored id, by which a stored submission's pinned version is known once its
 *                 reader has resolved it
 * @param number   the version number; a form's versions have distinct numbers
 * @param sections the version's sections
 * @param fields   the version's fields
 */
public record FormVersionDefinition(
    String id,
    int number,
    List<SectionDefinition> sections,
    List<FieldDefinition> fields) {

    public FormVersionDefinition {
        Objects.requireNonNull(id, "id");
        sections = List.copyOf(sections);
        fields = List.copyOf(fields);
    }

    /**
     * Reads one stored form version.
     *
     * @param formId        the form's stored id (section ids derive from it)
     * @param id            the form version's stored id
     * @param number        the version number
     * @param sections      the stored {@code sections} array; anything else holds no section
     * @param fields        the stored {@code fields} array; anything else holds no field
     * @param fieldIdForUid the stored id of the data element with a given uid, or {@code null} when there is none;
     *                      each field's {@code id} is a data element uid. A field whose data element has no id is
     *                      kept without an id, and its values are then unmapped, never refused
     * @param optionSetIdForUid the stored id of the option set with a given uid, or {@code null} when there is none;
     *                          a field's {@code optionSet} is an option set uid
     */
    public static FormVersionDefinition parse(
        String formId,
        String id,
        int number,
        JsonNode sections,
        JsonNode fields,
        Function<String, String> fieldIdForUid,
        Function<String, String> optionSetIdForUid) {
        List<SectionDefinition> sectionList = new ArrayList<>();
        if (sections != null && sections.isArray()) {
            for (JsonNode section : sections) {
                String path = text(section.get("path"));
                if (path == null) {
                    continue;
                }
                JsonNode repeatable = section.get("repeatable");
                sectionList.add(new SectionDefinition(
                    path,
                    repeatable != null && repeatable.asBoolean(false),
                    DerivedIds.sectionId(formId, path),
                    text(section.get("name")),
                    json(section.get("label")),
                    integer(section.get("order"))));
            }
        }
        List<FieldDefinition> fieldList = new ArrayList<>();
        if (fields != null && fields.isArray()) {
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
                String optionSetUid = text(field.get("optionSet"));
                String type = text(field.get("type"));
                fieldList.add(new FieldDefinition(
                    fieldId,
                    sectionPath == null ? name : sectionPath + "." + name,
                    sectionPath == null ? null : DerivedIds.sectionId(formId, sectionPath),
                    valueType(type),
                    type,
                    name,
                    json(field.get("label")),
                    optionSetUid == null ? null : optionSetIdForUid.apply(optionSetUid),
                    bool(field.get("mandatory")),
                    integer(field.get("order"))));
            }
        }
        return new FormVersionDefinition(id, number, sectionList, fieldList);
    }

    /** The path without its last dot-separated key; empty when it has one key. */
    static String parentPath(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(0, dot);
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    /** A stored value whole, in JSON text, or {@code null} when absent or the JSON null. */
    private static String json(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.toString();
    }

    /** A stored boolean, or {@code null} when absent or of another type. */
    private static Boolean bool(JsonNode node) {
        return node != null && node.isBoolean() ? node.booleanValue() : null;
    }

    /** A stored whole number that fits an int, or {@code null} when absent or anything else. */
    private static Integer integer(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
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
