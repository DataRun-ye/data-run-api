//package org.nmcpye.datarun.domainmapping.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.domainmapping.dto.DomainDto;
//import org.nmcpye.datarun.domainmapping.model.DataElementDomainMapping;
//import org.nmcpye.datarun.utils.IdempotencyKeyGenerator;
//import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
//import org.springframework.stereotype.Service;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author Hamza Assada
// * @since 23/09/2025
// */
//@Service
//@RequiredArgsConstructor
//public class MappingExecutor {
//    private final ObjectMapper json = new ObjectMapper();
//    private final LookupService lookupService;
//
//    /**
//     * Apply a single mapping record to a structural element_data_value -> returns list of DomainDto
//     */
//    public List<DomainDto> applyMapping(DataElementDomainMapping mapping, ElementDataValue edv, String edvStructuralKey, java.util.UUID etlRunId) {
//        // parse mapping.mappingExprJson -> simple ops list
//        try {
//            JsonNode root = json.readTree(mapping.getMappingExprJson());
//            // we expect {"ops": [ { "op":"extract", ...}, ... ], "to_domain":"person.age", ... }
//            JsonNode ops = root.path("ops");
//            // For minimal initial stage, support:
//            // extract -> value reference
//            // coerce -> type conversion (integer, number, boolean, date)
//            // lookup -> option or org unit lookup
//            // derive -> simple expression string (we'll do only trivial forms now)
//            Map<String, Object> context = new HashMap<>();
//            // initial extracted value placed in "value"
//            for (JsonNode op : ops) {
//                String name = op.path("op").asText();
//                switch (name) {
//                    case "extract":
//                        // :from can be "value" (default) or a path - but today edv already materialized
//                        context.put("value", extractValue(edv));
//                        break;
//                    case "coerce":
//                        String type = op.path("type").asText();
//                        context.put("value", coerce(context.get("value"), type, op.path("on_invalid").asText(null)));
//                        break;
//                    case "lookup":
//                        // expects "lookup_table" or "option_set_uid"
//                        String optionSetUid = op.path("option_set_uid").asText(null);
//                        if (optionSetUid != null && context.get("value") != null) {
//                            String code = String.valueOf(context.get("value"));
//                            String uid = lookupService.resolveOptionValueUid(optionSetUid, code);
//                            context.put("value_resolved_uid", uid);
//                        }
//                        break;
//                    case "derive":
//                        // very small derivation: support "gte" form e.g {"expr":"value >= 18","to":"person.is_adult"}
//                        // We'll implement trivial numeric compare like "value >= 18"
//                        String expr = op.path("expr").asText();
//                        Object derived = basicEvaluate(expr, context.get("value"));
//                        context.put("derived", derived);
//                        break;
//                    case "ignore":
//                        return Collections.emptyList();
//                    default:
//                        // unknown op -> ignore or throw? log and continue.
//                        break;
//                }
//            }
//            // Build DomainDto (single-output) using mapping.to_domain (mapping.domainConceptId)
//            DomainDto dto = DomainDto.builder()
//                .domainConceptId(mapping.getDomainConceptId())
//                .submissionUid(edv.getSubmissionUid())
//                .repeatInstanceId(edv.getRepeatInstanceId())
//                .valueText(context.get("value") == null ? null : String.valueOf(context.get("value")))
//                .valueNum(context.get("value") instanceof Number ? ((Number) context.get("value")).doubleValue() : null)
//                .valueBool(context.get("derived") instanceof Boolean ? (Boolean) context.get("derived") : (context.get("value") instanceof Boolean ? (Boolean) context.get("value") : null))
//                .valueArrayJson(edv.getValueArray())
//                .sourceElementDataValueId(edv.getId())
//                .etlRunId(etlRunId)
//                .mappingUid(mapping.getMappingUid())
//                .build();
//
//            // compute idempotency key (domain_key = mappingUid|edvStructuralKey)
//            String domainKey = IdempotencyKeyGenerator.domainKey(mapping.getMappingUid(), edvStructuralKey, null);
//            dto.setIdempotencyKey(domainKey);
//            return Collections.singletonList(dto);
//        } catch (Exception ex) {
//            throw new RuntimeException("Mapping execution failed", ex);
//        }
//    }
//
//    private Object extractValue(ElementDataValue edv) {
//        // prefer typed value columns
//        if (edv.getValueNum() != null) return edv.getValueNum();
//        if (edv.getValueBool() != null) return edv.getValueBool();
//        if (edv.getValueText() != null) return edv.getValueText();
//        if (edv.getValueArray() != null) return edv.getValueArray();
//        return null;
//    }
//
//    private Object coerce(Object value, String type, String onInvalid) {
//        if (value == null) return null;
//        try {
//            switch (type.toLowerCase()) {
//                case "integer":
//                    return Integer.parseInt(String.valueOf(value));
//                case "number":
//                    return Double.parseDouble(String.valueOf(value));
//                case "boolean":
//                    return Boolean.parseBoolean(String.valueOf(value));
//                case "text":
//                    return String.valueOf(value);
//                default:
//                    return value;
//            }
//        } catch (Exception ex) {
//            if ("null".equalsIgnoreCase(onInvalid)) return null;
//            if ("fail".equalsIgnoreCase(onInvalid)) throw ex;
//            return null;
//        }
//    }
//
//    private Object basicEvaluate(String expr, Object value) {
//        // Very small evaluator: support "value >= N" or "value == N"
//        try {
//            expr = expr.trim();
//            if (expr.contains(">=")) {
//                String[] parts = expr.split(">=");
//                double threshold = Double.parseDouble(parts[1].trim());
//                double v = Double.parseDouble(String.valueOf(value));
//                return v >= threshold;
//            }
//            if (expr.contains("==")) {
//                String[] parts = expr.split("==");
//                String rhs = parts[1].trim();
//                return String.valueOf(value).equals(rhs);
//            }
//        } catch (Exception e) {
//            // degrade to null
//        }
//        return null;
//    }
//}
