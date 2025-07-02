//package org.nmcpye.datarun.utils;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.*;
//import org.nmcpye.datarun.datatemplateelement.AbstractElement;
//import org.nmcpye.datarun.datatemplateelement.DataFieldRule;
//import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
//import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
//import org.springframework.stereotype.Service;
//
///**
// * @author Hamza Assada 24/06/2025 (7amza.it@gmail.com)
// */
//@Service
//public class FormSchemaGenerator {
//
//    private final ObjectMapper MAPPER = new ObjectMapper();
//
//    /**
//     * Generate a JSON Schema (as JsonNode) for validating instances of this template.
//     */
//    public ObjectNode generateRuntimeSchema(DataTemplateInstanceDto template) {
//        ObjectNode schema = MAPPER.createObjectNode();
//        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
//        schema.put("type", "object");
//
//        ObjectNode props = MAPPER.createObjectNode();
//        ArrayNode required = MAPPER.createArrayNode();
//
//        for (FormDataElementConf elem : template.getFields()) {
//            String id = elem.getId();
//            ObjectNode fieldSchema = buildFieldSchema(elem);
//            props.set(id, fieldSchema);
//
//            if (Boolean.TRUE.equals(elem.getMandatory())) {
//                required.add(id);
//            }
//        }
//
//        schema.set("properties", props);
//        if (required.size() > 0) {
//            schema.set("required", required);
//        }
//
//        ArrayNode allOf = MAPPER.createArrayNode();
//        for (DataFieldRule rule : template.getRules()) {
//            ObjectNode cond = MAPPER.createObjectNode();
//            cond.set("if", buildIfClause(rule));
//            cond.set("then", buildThenClause(rule));
//            allOf.add(cond);
//        }
//        if (allOf.size() > 0) {
//            schema.set("allOf", allOf);
//        }
//
//        return schema;
//    }
//
//    private ObjectNode buildFieldSchema(FormDataElementConf elem) {
//        ObjectNode node = MAPPER.createObjectNode();
//        switch (elem.getType()) {
//            case "Text":
//                node.put("type", "string");
//                break;
//            case "Number":
//                node.put("type", "number");
//                break;
//            case "SelectOne":
//                node.set("enum", MAPPER.valueToTree(elem.getOptions()));
//                break;
//            case "SelectMulti":
//                node.put("type", "array");
//                ObjectNode items = MAPPER.createObjectNode();
//                items.set("enum", MAPPER.valueToTree(elem.getOptions()));
//                node.set("items", items);
//                if (Boolean.TRUE.equals(elem.getMandatory())) {
//                    node.put("minItems", 1);
//                }
//                node.put("uniqueItems", true);
//                break;
//            case "Team":
//            case "OrgUnit":
//                node.put("type", "string");
//                node.put("format", "ulid");
//                break;
//            default:
//                node.put("type", "string");
//        }
//        return node;
//    }
//
//    private ObjectNode buildIfClause(DataFieldRule rule) {
//        ObjectNode ifNode = MAPPER.createObjectNode();
//        ObjectNode props = MAPPER.createObjectNode();
//        String[] parts = rule.getExpression().split("==");
//        String field = parts[0].trim();
//        String value = parts[1].trim().replaceAll("'", "");
//        ObjectNode eq = MAPPER.createObjectNode();
//        eq.put("const", value);
//        ObjectNode fieldNode = MAPPER.createObjectNode().set("properties",
//            MAPPER.createObjectNode().set(field, eq));
//        ifNode.setAll(fieldNode);
//        return ifNode;
//    }
//
//    private ObjectNode buildThenClause(DataFieldRule rule) {
//        ObjectNode thenNode = MAPPER.createObjectNode();
//        switch (rule.getAction()) {
//            case "makeMandatory":
//                thenNode.set("required",
//                    MAPPER.createArrayNode().add(rule.getTargetFieldId()));
//                break;
//            case "hide":
//                thenNode.set("properties",
//                    MAPPER.createObjectNode()
//                        .set(rule.getTargetFieldId(),
//                            MAPPER.createObjectNode()
//                                .put("const", null)));
//                break;
//            default:
//        }
//        return thenNode;
//    }
//}
//
