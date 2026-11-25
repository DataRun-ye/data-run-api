package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Synthetic forms written the way {@code data_template_version} stores them, with invented ids. A field's stored
 * {@code id} is its data element's uid, and its stored {@code path} ends with that uid; the data element's id is the
 * uid's entry in {@link #FIELD_IDS}.
 */
final class StoredForms {

    static final ObjectMapper JSON = new ObjectMapper();

    /** The uid of the form the synthetic submissions name; the normalizer only carries it. */
    static final String FORM_UID = "fmUid000001";

    /** Data element uid to its stored id. */
    static final Map<String, String> FIELD_IDS = new HashMap<>();

    /** Option set uid to its stored id; an option set not registered here does not exist. */
    static final Map<String, String> OPTION_SET_IDS = new HashMap<>();

    private StoredForms() {
    }

    /** The stored id of the data element with this uid (registered on first use). */
    static String fieldId(String uid) {
        return FIELD_IDS.computeIfAbsent(uid, u -> String.format("01J%023d", Math.abs(u.hashCode()) % 1_000_000_000L));
    }

    static VersionBuilder version(String id, String uid, int number) {
        return new VersionBuilder(id, uid, number);
    }

    /** JSON text written with single quotes, as a stored document's text. */
    static String text(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }

    static JsonNode json(String text) {
        try {
            return JSON.readTree(text.replace('\'', '"'));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    static final class VersionBuilder {
        private final String id;
        private final String uid;
        private final int number;
        private final ArrayNode sections = JSON.createArrayNode();
        private final ArrayNode fields = JSON.createArrayNode();

        private VersionBuilder(String id, String uid, int number) {
            this.id = id;
            this.uid = uid;
            this.number = number;
        }

        VersionBuilder section(String path, boolean repeating) {
            ObjectNode section = sections.addObject();
            String name = path.substring(path.lastIndexOf('.') + 1);
            section.put("id", name);
            section.put("name", name);
            section.put("path", path);
            section.put("repeatable", repeating);
            if (path.contains(".")) {
                section.put("parent", path.substring(0, path.lastIndexOf('.')));
            }
            return this;
        }

        /** A field; {@code sectionPath} is {@code null} for a field outside every section. */
        VersionBuilder field(String dataElementUid, String sectionPath, String name, String type) {
            fieldId(dataElementUid);
            ObjectNode field = fields.addObject();
            field.put("id", dataElementUid);
            field.put("name", name);
            field.put("path", sectionPath == null ? dataElementUid : sectionPath + "." + dataElementUid);
            field.put("type", type);
            return this;
        }

        /** A field whose data element no longer exists: its uid has no id. */
        VersionBuilder fieldWithoutDataElement(String dataElementUid, String sectionPath, String name, String type) {
            ObjectNode field = fields.addObject();
            field.put("id", dataElementUid);
            field.put("name", name);
            field.put("path", sectionPath == null ? dataElementUid : sectionPath + "." + dataElementUid);
            field.put("type", type);
            return this;
        }

        FormVersionDefinition build(String formId) {
            return FormVersionDefinition.parse(formId, id, uid, number, sections, fields, FIELD_IDS::get,
                OPTION_SET_IDS::get);
        }
    }
}
