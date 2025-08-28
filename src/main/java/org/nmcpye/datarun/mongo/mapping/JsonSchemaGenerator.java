//package org.nmcpye.datarun.mongo.mapping;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.swagger.v3.oas.models.media.JsonSchema;
//import io.swagger.v3.oas.models.media.ObjectSchema;
////import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
////import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
////import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
////import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
//import java.io.File;
//import java.io.IOException;
//
//public class JsonSchemaGenerator {
//
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
//
//    public static void main(String[] args) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode formTemplate = mapper.readTree(new File("formTemplate.json"));
//        JsonSchema schema = generateSchema(formTemplate);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
//    }
//}
