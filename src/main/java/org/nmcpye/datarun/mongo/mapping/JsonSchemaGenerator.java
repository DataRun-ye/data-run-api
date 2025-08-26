package org.nmcpye.datarun.mongo.mapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonSchemaGenerator {

//    public static JsonSchema generateSchema(JsonNode formTemplate) {
//        ObjectMapper mapper = new ObjectMapper();
//        SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
//
//        ObjectSchema schema = new ObjectSchema();
//        schema.setId(formTemplate.get("uid").asText());
//        schema.setTitle(formTemplate.get("name").asText());
//
//        formTemplate.get("fields").forEach(field -> {
//            String fieldName = field.get("name").asText();
//            String fieldType = field.get("type").asText();
//
//            if (fieldType.equals("Section") || fieldType.equals("RepeatableSection")) {
//                ObjectSchema sectionSchema = new ObjectSchema();
//                field.get("fields").forEach(subField -> {
//                    sectionSchema.putProperty(subField.get("name").asText(), getFieldSchema(subField));
//                });
//                schema.putProperty(fieldName, sectionSchema);
//            } else {
//                schema.putProperty(fieldName, getFieldSchema(field));
//            }
//        });
//
//        return schema;
//    }
//
//    private static JsonSchema getFieldSchema(JsonNode field) {
//        String fieldType = field.get("type").asText();
//        switch (fieldType) {
//            case "SelectOne":
//            case "ScannedCode":
//                return new StringSchema();
//            default:
//                return new StringSchema();
//        }
//    }

    /**
     * Creates a "signature" of a DDL string that is resilient to formatting
     * and column order changes.
     */
    private static String generateDdlSignature(String ddl) {
        // 1. Normalize the entire string to handle whitespace and casing
        String normalizedDdl = ddl.toLowerCase()
                .replaceAll("--.*", "")       // Remove SQL comments
                .replaceAll("\\s+", " ") // Collapse all whitespace to single spaces
                .trim();

        // 2. Extract the most volatile part: the column definitions
        // This regex captures the text between SELECT and FROM
        Pattern pattern = Pattern.compile("select(.*?)from");
        Matcher matcher = pattern.matcher(normalizedDdl);

        if (!matcher.find()) {
            // If the pattern fails, fallback to the normalized string. This shouldn't happen.
            return normalizedDdl;
        }

        String columnPart = matcher.group(1);

        // 3. Create an order-independent representation of the columns
        List<String> columns = Arrays.stream(columnPart.split(","))
                .map(String::trim)
                .sorted() // Sort alphabetically to make order irrelevant
                .collect(Collectors.toList());

        // 4. Reconstruct the signature using the sorted columns and the rest of the DDL
        String stableColumnPart = String.join(",", columns);
        String signature = matcher.replaceFirst("select " + stableColumnPart + " from");

        return signature;
    }

    public static void main(String[] args) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode formTemplate = mapper.readTree(new File("formTemplate.json"));
//        JsonSchema schema = generateSchema(formTemplate);
        String old = """
                 SELECT ev.id AS value_id,
                    ev.submission_id,
                    sub.form AS form_template_id,
                    sub.form_version AS form_version_id,
                    ev.element_label,
                    child_ri.repeat_section_label,
                    child_ri.id AS repeat_instance_id,
                    parent_ri.repeat_section_label AS parent_repeat_section_label,
                    ev.assignment_id,
                    ev.team_id,
                    ev.org_unit_id,
                    ev.activity_id,
                    ev.element_id,
                    ev.option_id,
                    sub.finished_entry_time AS submission_completed_at,
                    child_ri.repeat_path,
                    child_ri.category_id,
                    child_ri.category_kind,
                    parent_ri.id AS parent_repeat_instance_id,
                    parent_ri.category_id AS parent_category_id,
                    de.name AS element_name,
                    de.type AS element_value_type,
                    ov.name AS option_name,
                    ov.code AS option_code,
                    child_ri.category_name,
                    parent_ri.category_name AS parent_category_name,
                    ev.value_num,
                    ev.value_text,
                    ev.value_bool,
                    ev.value_ts,
                    ev.deleted_at
                   FROM (((((element_data_value ev
                     JOIN data_submission sub ON (((ev.submission_id)::text = (sub.uid)::text)))
                     LEFT JOIN data_element de ON (((ev.element_id)::text = (de.uid)::text)))
                     LEFT JOIN option_value ov ON (((ev.option_id)::text = (ov.id)::text)))
                     LEFT JOIN repeat_instance child_ri ON (((ev.repeat_instance_id)::text = (child_ri.id)::text)))
                     LEFT JOIN repeat_instance parent_ri ON (((child_ri.parent_repeat_instance_id)::text = (parent_ri.id)::text)));
                """;
        String string = generateDdlSignature(old);
        System.out.println(string);
    }
}
