//package org.nmcpye.datarun.etl.service.impl;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.etl.dto.ExtractedValue;
//import org.nmcpye.datarun.outbox.dto.OutboxDto;
//import org.nmcpye.datarun.etl.model.TallCanonicalRow;
//import org.nmcpye.datarun.etl.service.TransformService;
//import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class TransformServiceImpl implements TransformService {
//
//    private static final Logger log = LoggerFactory.getLogger(TransformServiceImpl.class);
//
//    private static final int VALUE_TEXT_MAX = 4000;
//    private static final int VALUE_JSON_MAX = 10000;
//
//    @Override
//    public List<TallCanonicalRow> transform(OutboxDto outbox) throws Exception {
//        return List.of();
//    }
//
//    // --- simplified transform using refactored helpers ---
//    public List<TallCanonicalRow> transformSimplified(JsonNode root, List<TemplateElement> elements) {
//        List<TallCanonicalRow> rows = new ArrayList<>();
//        Instant now = Instant.now();
//
//        for (TemplateElement element : elements) {
//            String path = element.getJsonDataPath();
//            if (path == null || path.isBlank()) continue;
//
//            List<PathHit> hits = resolvePathHitsFlexible(root, path); // simplified resolver that supports '*' tokens
//            for (PathHit hit : hits) {
//                JsonNode node = hit.node;
//                if (node == null || node.isMissingNode() || node.isNull()) continue;
//
//                // proper parentRepeat handling: nearest array element is the parent repeat
//                JsonNode parentRepeat = hit.parentRepeatNode;
//
//                String repeatInstanceId = firstNonNullText(parentRepeat, "_id", node, "_id");
//                String parentInstanceId = firstNonNullText(parentRepeat, "_parentId", node, "_parentId");
//                Integer repeatIndex = resolveRepeatIndex(parentRepeat, node, hit.arrayIndex);
//
//                ExtractedValue v = extractValue(node);
//                if (v.isEmpty()) continue;
//
//                TallCanonicalRow r = TallCanonicalRow.builder()
//                    .canonicalElementId(element.getCanonicalElementId())
//                    .elementPath(hit.elementPath)
//                    .repeatInstanceId(repeatInstanceId)
//                    .parentInstanceId(parentInstanceId)
//                    .repeatIndex(repeatIndex)
//                    .valueText(v.valueText())
//                    .valueNumber(v.valueNumber())
//                    .valueBool(v.valueBool())
//                    .valueJson(v.valueJson())
//                    .createdAt(now)
//                    .updatedAt(now)
//                    .build();
//
//                rows.add(r);
//            }
//        }
//        return rows;
//    }
//
//    // --- helper to pick first non-null text from parent then node ---
//    private String firstNonNullText(JsonNode maybeParent, String parentField, JsonNode node, String nodeField) {
//        if (maybeParent != null && !maybeParent.isMissingNode()) {
//            String p = maybeParent.path(parentField).asText(null);
//            if (p != null) return p;
//        }
//        return node.path(nodeField).asText(null);
//    }
//
//    private Integer resolveRepeatIndex(JsonNode parentRepeat, JsonNode node, Integer fallbackIndex) {
//        if (parentRepeat != null && parentRepeat.has("_index") && parentRepeat.get("_index").canConvertToInt()) {
//            return parentRepeat.get("_index").intValue();
//        }
//        if (node.has("_index") && node.get("_index").canConvertToInt()) {
//            return node.get("_index").intValue();
//        }
//        return fallbackIndex;
//    }
//
//    // --- simpler, robust resolvePathHitsFlexible ---
//// Supports dotted segments, and if a segment is '*', it will expand array/object children.
//// Behavior: when encountering an array, the element's parentRepeatNode is set to the element itself.
//    private List<PathHit> resolvePathHitsFlexible(JsonNode root, String dottedPath) {
//        List<PathHit> current = new ArrayList<>();
//        current.add(new PathHit(root, null, "", null));
//
//        String[] segments = dottedPath.split("\\.");
//        for (String seg : segments) {
//            List<PathHit> next = new ArrayList<>();
//            for (PathHit ph : current) {
//                JsonNode node = ph.node;
//                if (node == null || node.isMissingNode() || node.isNull()) continue;
//
//                JsonNode child = node.get(seg);
//                if (child == null || child.isMissingNode() || child.isNull()) continue;
//
//                if (child.isArray()) {
//                    int size = child.size();
//                    for (int i = 0; i < size; i++) {
//                        JsonNode elem = child.get(i);
//                        String newPath = buildPath(ph.elementPath, seg, i);
//                        // **important fix**: nearest parent repeat is this element
//                        next.add(new PathHit(elem, i, newPath, elem));
//                    }
//                } else {
//                    String newPath = buildPath(ph.elementPath, seg, -1);
//                    next.add(new PathHit(child, ph.arrayIndex, newPath, ph.parentRepeatNode));
//                }
//            }
//            current = next;
//            if (current.isEmpty()) break;
//        }
//        return current;
//    }
//
//    // --- compact but safe extractValue implementation (single serialization for containers) ---
//    private ExtractedValue extractValue(JsonNode node) {
//        String valueText = null;
//        Boolean valueBool = null;
//        BigDecimal valueNumber = null;
//        String valueJson = null;
//
//        if (node.isNumber()) {
//            valueNumber = node.decimalValue();
//            valueText = node.asText();
//        } else if (node.isBoolean()) {
//            valueBool = node.asBoolean();
//            valueText = node.asText();
//        } else if (node.isTextual()) {
//            valueText = node.asText();
//        } else if (node.isNull()) {
//            valueText = null;
//        } else if (node.isContainerNode()) { // object or array
//            String raw = node.toString(); // single serialization
//            if (raw.length() > VALUE_JSON_MAX) {
//                valueJson = raw.substring(0, VALUE_JSON_MAX) + "...(truncated)";
//                valueText = raw.substring(0, Math.min(VALUE_TEXT_MAX, raw.length())) + "...(truncated)";
//            } else {
//                valueJson = raw;
//                // for containers we typically do not want full text, so keep valueText null (or a short preview)
//                if (raw.length() > VALUE_TEXT_MAX) valueText = raw.substring(0, VALUE_TEXT_MAX) + "...(truncated)";
//                else valueText = null;
//            }
//        } else {
//            valueText = node.asText(null);
//        }
//
//        if (valueText != null && valueText.length() > VALUE_TEXT_MAX) {
//            valueText = valueText.substring(0, VALUE_TEXT_MAX) + "...(truncated)";
//        }
//        return new ExtractedValue(valueText, valueBool, valueNumber, valueJson);
//    }
//
//    // ///////////////////////////////////////////////////////
//    // ///////////////////////////////////////////////////////
//    // ///////////////////////////////////////////////////////
//
//    public List<TallCanonicalRow> transform(JsonNode root, List<TemplateElement> elements) {
//        List<TallCanonicalRow> result = new ArrayList<>();
//
//        Instant now = Instant.now();
//
//        for (TemplateElement element : elements) {
//            String jsonPath = element.getJsonDataPath();
//            if (jsonPath == null || jsonPath.isBlank()) continue;
//
//            List<PathHit> hits = resolvePathHits(root, jsonPath);
//
//            if (element.isRepeat() && (hits.isEmpty())) {
//                log.debug("Transform: repeat TemplateElement {} has no array hits in payload",
//                    element.getCanonicalElementId());
//            }
//
//            for (PathHit hit : hits) {
//                JsonNode node = hit.node;
//                if (node == null || node.isMissingNode() || node.isNull()) continue;
//
//                // Prefer metadata from parentRepeatNode when present (repeat object holds _id/_parentId/_index)
//                JsonNode parentRepeat = hit.parentRepeatNode;
//
//                String repeatInstanceId = getText(parentRepeat, "_id");
//                if (repeatInstanceId == null) repeatInstanceId = getText(node, "_id");
//
//                String parentInstanceId = getText(parentRepeat, "_parentId");
//                if (parentInstanceId == null) parentInstanceId = getText(node, "_parentId");
//
//                Integer repeatIndex = null;
//                if (parentRepeat != null && parentRepeat.has("_index") && parentRepeat.get("_index").canConvertToInt()) {
//                    repeatIndex = parentRepeat.get("_index").intValue();
//                } else if (node.has("_index") && node.get("_index").canConvertToInt()) {
//                    repeatIndex = node.get("_index").intValue();
//                } else if (hit.arrayIndex != null) {
//                    repeatIndex = hit.arrayIndex;
//                }
//
//                // Derive values
//                String valueText = null;
//                Boolean valueBool = null;
//                BigDecimal valueNumber = null;
//                String valueJson = null;
//
//                if (node.isNumber()) {
//                    valueNumber = node.decimalValue();
//                    valueText = node.asText();
//                } else if (node.isBoolean()) {
//                    valueText = node.asText();
//                    valueBool = node.asBoolean();
//                } else if (node.isTextual() || node.isNull()) {
//                    valueText = node.isNull() ? null : node.asText();
//                } else if (node.isObject() || node.isArray()) {
//                    String raw = node.toString();
//                    if (raw.length() > VALUE_JSON_MAX) {
//                        valueJson = raw.substring(0, VALUE_JSON_MAX) + "...(truncated)";
//                        valueText = raw.substring(0, VALUE_TEXT_MAX) + "...(truncated)";
//                    } else {
//                        valueJson = raw;
//                    }
//                } else {
//                    valueText = node.asText(null);
//                }
//
//                if (valueText != null && valueText.length() > VALUE_TEXT_MAX) {
//                    valueText = valueText.substring(0, VALUE_TEXT_MAX) + "...(truncated)";
//                }
//
//                boolean emptyText = (valueText == null || valueText.isEmpty());
//                boolean emptyNumber = (valueNumber == null);
//                boolean emptyJson = (valueJson == null || valueJson.isEmpty());
//                if (emptyText && emptyNumber && emptyJson) continue;
//
//                TallCanonicalRow row = TallCanonicalRow.builder()
//                    .canonicalElementId(element.getCanonicalElementId())
//                    .elementPath(hit.elementPath)
//                    .repeatInstanceId(repeatInstanceId)
//                    .parentInstanceId(parentInstanceId)
//                    .repeatIndex(repeatIndex)
//                    .valueText(valueText)
//                    .valueNumber(valueNumber)
//                    .valueBool(valueBool)
//                    .valueJson(valueJson)
//                    .createdAt(now)
//                    .updatedAt(now)
//                    .build();
//
//                result.add(row);
//            }
//        }
//
//        return result;
//    }
//
//    // --------------------
//    // Helpers
//    // --------------------
//
//    private String getText(JsonNode node, String field) {
//        if (node == null || node.isMissingNode()) return null;
//        JsonNode v = node.get(field);
//        if (v == null || v.isMissingNode() || v.isNull()) return null;
//        return v.asText();
//    }
//
//    /**
//     * Resolve dotted path into a list of PathHit.
//     * - when encountering arrays, iterate elements and continue traversal for each.
//     * - preserves the array element node as parentRepeatNode so leaf fields can inherit repeat metadata.
//     * - elementPath normalized to "arr[0].field" style.
//     * <p>
//     * to the child segment (avoid producing "arr[0].field[0]"). Only the array segment itself carries the index.
//     */
//    private List<PathHit> resolvePathHits(JsonNode root, String dottedPath) {
//        List<PathHit> current = new ArrayList<>();
//        current.add(new PathHit(root, null, "", null)); // start with no parent repeat
//
//        String[] segments = dottedPath.split("\\.");
//        for (String segment : segments) {
//            List<PathHit> next = new ArrayList<>();
//            for (PathHit ph : current) {
//                JsonNode node = ph.node;
//                if (node == null || node.isMissingNode() || node.isNull()) continue;
//
//                JsonNode child = node.get(segment);
//                if (child == null || child.isMissingNode() || child.isNull()) continue;
//
//                if (child.isArray()) {
//                    int size = child.size();
//                    for (int i = 0; i < size; i++) {
//                        JsonNode elem = child.get(i);
//                        // array element path should include the index on the array segment
//                        String newPath = buildPath(ph.elementPath, segment, i);
//                        // If we're already under a repeat element, keep that node as parentRepeatNode;
//                        // otherwise the element itself becomes the parentRepeatNode (for top-level arrays).
//                        JsonNode parentRepeatForElem = ph.parentRepeatNode != null ? ph.parentRepeatNode : elem;
//                        next.add(new PathHit(elem, i, newPath, parentRepeatForElem));
//
////                        // the element itself becomes the parentRepeatNode for downstream child fields
////                        // propagate parentRepeatNode if any (...)
////                        next.add(new PathHit(elem, i, newPath, elem));
//                    }
//                } else {
//                    // child is a scalar/object under the current node.
//                    // NOTE: do NOT re-apply array index here — if the parent was an array element,
//                    // ph.elementPath already contains the "[i]" and we should not append index to child.
//                    String newPath = buildPath(ph.elementPath, segment, -1);
//                    next.add(new PathHit(child, ph.arrayIndex, newPath, ph.parentRepeatNode));
//                }
//            }
//            current = next;
//            if (current.isEmpty()) break;
//        }
//
//        return current;
//    }
//
//    /**
//     * Build normalized element path like:
//     * - "larvalHabitats[0]" for an array element
//     * - "larvalHabitats[0].owner_name" for a child field
//     * - "main.location_name" for a scalar path
//     * <p>
//     * arrayIndex == -1 means: do not append [index] for this segment.
//     */
//    private String buildPath(String base, String seg, int arrayIndex) {
//        StringBuilder sb = new StringBuilder();
//        if (base != null && !base.isEmpty()) {
//            sb.append(base);
//            sb.append(".");
//        }
//        sb.append(seg);
//        if (arrayIndex >= 0) sb.append("[").append(arrayIndex).append("]");
//        return sb.toString();
//    }
//
//    private static class PathHit {
//        final JsonNode node;
//        final Integer arrayIndex; // index if array traversed at last segment (may be null)
//        final String elementPath;
//        final JsonNode parentRepeatNode; // the array element node that acts as parent for leaf fields
//
//        PathHit(JsonNode node, Integer arrayIndex, String elementPath, JsonNode parentRepeatNode) {
//            this.node = node;
//            this.arrayIndex = arrayIndex;
//            this.elementPath = elementPath;
//            this.parentRepeatNode = parentRepeatNode;
//        }
//    }
//}
//
