//package org.nmcpye.datarun.jpa.etl.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
//import org.nmcpye.datarun.datatemplateelement.AbstractElement;
//import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
//import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
//import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
//import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
//import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
//import org.nmcpye.datarun.jpa.etl.exception.MissingRepeatUidException;
//import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
//import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
//import org.nmcpye.datarun.jpa.option.service.OptionService;
//import org.nmcpye.datarun.security.SecurityUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
///**
// * Normalizer working with JsonNode formData instead of Map.
// * Behavior preserves the Map-based implementation:
// * <p>
// * - generates missing repeat UIDs (optionally) and writes them back into a deep-copied formData
// * <p>
// * - extracts repeat instances and value rows
// * <p>
// * - handles multi-select arrays, primitives, nested objects and repeat sections
// *
// * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
// */
//@SuppressWarnings("unused")
//public class SubmissionJsonNodeNormalizer implements DataNormalizer<JsonNode> {
//
//    private final Logger log = LoggerFactory.getLogger(SubmissionNormalizer.class);
//
//    private final TemplateElementMap elementMap;
//    private final OptionService optionService;
//    private final ObjectMapper objectMapper;
//    private final boolean generateMissingRepeatUids;
//
//    public SubmissionJsonNodeNormalizer(OptionService optionService,
//                                TemplateElementMap elementMap,
//                                ObjectMapper objectMapper,
//                                boolean generateMissingRepeatUids) {
//        this.elementMap = elementMap;
//        this.optionService = optionService;
//        this.objectMapper = objectMapper;
//        this.generateMissingRepeatUids = generateMissingRepeatUids;
//    }
//
//    @Override
//    public NormalizedSubmission normalize(DataSubmission submission) {
//        // Use a deep copy of the incoming JsonNode so we can modify UIDs safely
//        JsonNode original = submission.getFormData();
//        ObjectNode rootCopy;
//        if (original == null || original.isNull()) {
//            rootCopy = objectMapper.createObjectNode(); // empty object
//        } else if (original.isObject()) {
//            rootCopy = ((ObjectNode) original).deepCopy();
//        } else {
//            // If root is array or primitive, wrap it into an object under "value"
//            rootCopy = objectMapper.createObjectNode();
//            rootCopy.set("value", original.deepCopy());
//        }
//
//        final var ns = new NormalizedSubmission(
//                submission.getUid(),
//                submission.getTemplateUid(),
//                submission.getAssignment()
//        );
//
//        // walk root
//        extractNode(rootCopy, /*currentPath*/ null,
//                ns, submission, /*repeatId*/ null, /*repeatIndex*/ null, /*categoryId*/ null);
//
//        // If we generated missing repeat UIDs and mutated the rootCopy (we always set when generateMissingRepeatUids==true),
//        // set it back on the submission so it gets persisted (important for later passes).
//        if (generateMissingRepeatUids) {
//            submission.setFormData(rootCopy);
//        }
//
//        return ns;
//    }
//
//    /**
//     * Recursively traverse the JSON tree.
//     * node is guaranteed to be an ObjectNode for fields traversal.
//     */
//    @Override
//    public void extractNode(
//            JsonNode node,
//            String currentPath,
//            NormalizedSubmission ns,
//            DataSubmission submission,
//            String currentRepeatId,
//            Long currentRepeatIndex,
//            String currentCategoryId) {
//
//        if (node == null || node.isNull()) return;
//        if (!node.isObject()) {
//            // nothing to do if the node isn't an object at this level
//            return;
//        }
//
//        ObjectNode objectNode = (ObjectNode) node;
//        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
//
//        while (fields.hasNext()) {
//            Map.Entry<String, JsonNode> entry = fields.next();
//            String key = entry.getKey();
//            JsonNode rawValue = entry.getValue();
//            String childPath = (currentPath == null || currentPath.isEmpty()) ? key : currentPath + "." + key;
//
//            AbstractElement element = elementMap.getElementPathMapCache().get(childPath);
//            if (element == null) {
//                // Unknown element — same behavior as Map version: skip
//                continue;
//            }
//
//            // --- repeatable section (array of objects) branch ---
//            if (element instanceof FormSectionConf sectionConf
//                    && Boolean.TRUE.equals(sectionConf.getRepeatable())
//                    && rawValue != null && rawValue.isArray()) {
//
//                ArrayNode array = (ArrayNode) rawValue;
//                long idx = 0L;
//                for (int i = 0; i < array.size(); i++) {
//                    JsonNode itemNode = array.get(i);
//                    if (!itemNode.isObject()) {
//                        // Defensive: skip non-object items (Map version assumed list-of-maps)
//                        idx++;
//                        continue;
//                    }
//                    ObjectNode itemObj = (ObjectNode) itemNode.deepCopy(); // mutable copy
//
//                    // get existing UID if present (prefers _uid)
//                    String repeatId = getTextOrNull(itemObj.get("_uid"));
//                    if (repeatId == null) {
//                        if (generateMissingRepeatUids) {
//                            repeatId = CodeGenerator.ULIDGenerator.nextString();
//                            itemObj.put("_uid", repeatId); // propagate back into copy
//                        } else {
//                            // same semantics as original: throw exception listing missing repeat UIDs
//                            throw new MissingRepeatUidException(List.of(
//                                    new MissingRepeatUidException.MissingRepeatUid(childPath, (int) idx)
//                            ));
//                        }
//                    }
//
//                    // Attach parent/submission metadata (propagate into copy)
//                    if (!itemObj.has("_parentId")) itemObj.set("_parentId",
//                            objectMapper.valueToTree(submission.getUid()));
//                    itemObj.put("_submissionUid", submission.getUid());
//                    if (!itemObj.has("_index")) itemObj.put("_index", (int) (idx + 1));
//
//                    // Persist mutated item back into array (replace position)
//                    array.set(i, itemObj);
//
//                    // create RepeatInstance and register it
//                    RepeatInstance ri = RepeatInstance.builder()
//                            .id(repeatId)
//                            .submission(ns.getSubmissionId())
//                            .repeatPath(childPath)
//                            .repeatIndex(idx)
//                            .clientUpdatedAt(submission.getFinishedEntryTime())
//                            .createdDate(submission.getCreatedDate())
//                            .lastModifiedDate(submission.getLastModifiedDate())
//                            .createdBy(SecurityUtils.getCurrentUserLoginOrThrowSafe()) // adapt to your helper
//                            .lastModifiedBy(SecurityUtils.getCurrentUserLoginOrThrowSafe())
//                            .build();
//
//                    ns.addRepeatInstance(ri);
//                    ns.addIncomingUid(childPath, repeatId);
//
//                    // extract category id for this item if configured
//                    String categoryValue = extractCategoryIdForItem(itemObj, childPath);
//
//                    // recurse into the item
//                    extractNode(itemObj, childPath, ns, submission, repeatId, idx, categoryValue);
//                    idx++;
//                }
//
//                // Note: array was modified in-place on the deepCopy rootCopy,
//                // and will be reflected when we later set submission.formData = rootCopy
//                continue;
//            }
//
//            // --- nested object branch ---
//            if (rawValue != null && rawValue.isObject()) {
//                extractNode(rawValue, childPath, ns, submission, currentRepeatId, currentRepeatIndex, currentCategoryId);
//                continue;
//            }
//
//            // --- array branch (multi-select array of primitives) ---
//            if (rawValue != null && rawValue.isArray() && element instanceof FormDataElementConf field) {
//                ArrayNode arr = (ArrayNode) rawValue;
//
//                String elementId = field.getId();
//                String optionSetId = field.getOptionSet(); // may be null
//
//                if (arr.size() == 0) {
//                    ns.markMultiSelectExplicitlyEmpty(currentRepeatId, elementId);
//                    continue;
//                }
//
//                // Normalize codes: array elements are expected to be primitives (string/number)
//                List<String> codes = new ArrayList<>();
//                for (JsonNode v : arr) {
//                    if (v == null || v.isNull()) continue;
//                    codes.add(v.asText().trim());
//                }
//
//                // validate and map codes -> option ids (reuses your service)
//                Map<String, String> codeToOptionId = optionService.validateAndMapOptionCodes(codes, optionSetId);
//
//                for (String code : codes) {
//                    String optionId = codeToOptionId.get(code);
//                    SubmissionValueRow row = SubmissionValueRow.builder()
//                            .element(elementId)
//                            .submission(ns.getSubmissionId())
//                            .repeatInstance(currentRepeatId)
//                            .category(currentCategoryId)
//                            .assignment(ns.getAssignmentId())
//                            .template(ns.getTemplateId())
//                            .option(optionId)
//                            .valueText(code)
//                            .createdDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
//                            .lastModifiedDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
//                            .build();
//
//                    ns.addValueRow(row);
//                    ns.addMultiSelectIdentity(currentRepeatId, elementId, optionId);
//                }
//                continue;
//            }
//
//            // --- primitive (single-value) branch ---
//            String elementId = resolveElementId(element);
//
//            SubmissionValueRow row = SubmissionValueRow.builder()
//                    .element(elementId)
//                    .submission(ns.getSubmissionId())
//                    .repeatInstance(currentRepeatId)
//                    .category(currentCategoryId)
//                    .assignment(ns.getAssignmentId())
//                    .template(ns.getTemplateId())
//                    .createdDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
//                    .lastModifiedDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
//                    .build();
//
//            // set typed values
//            if (rawValue == null || rawValue.isNull()) {
//                row.setValueText(null);
//            } else if (rawValue.isNumber()) {
//                // preserve decimal precision by using text -> BigDecimal
//                row.setValueNumber(new BigDecimal(rawValue.asText()));
//            } else if (rawValue.isBoolean()) {
//                row.setValueBoolean(rawValue.asBoolean());
//            } else {
//                // fallback to string text
//                row.setValueText(rawValue.asText());
//            }
//
//            ns.addValueRow(row);
//        }
//    }
//
//    /**
//     * Extracts category id for a repeated item. Mirrors the Map-based logic:
//     * - find the configured category element id for the repeat path
//     * - compute relative path from repeatPath to the category field
//     * - get the value under that relative path within the item node
//     */
//    private String extractCategoryIdForItem(JsonNode itemNode, String repeatPath) {
//        String categoryElementId = elementMap.getRepeatCategoryElementMapCache().get(repeatPath);
//        if (categoryElementId == null) return null;
//
//        String categoryFullPath = elementMap.getFieldElementReversePathMap().get(categoryElementId);
//        if (categoryFullPath == null) return null;
//
//        String relative;
//        if (categoryFullPath.startsWith(repeatPath + ".")) {
//            relative = categoryFullPath.substring(repeatPath.length() + 1);
//        } else {
//            relative = categoryFullPath;
//        }
//
//        JsonNode found = getNestedNode(itemNode, relative);
//        return found == null || found.isNull() ? null : found.asText();
//    }
//
//    /**
//     * Resolve element id similar to the Map version.
//     */
//    private String resolveElementId(AbstractElement element) {
//        if (element instanceof FormDataElementConf) {
//            return element.getId();
//        } else if (element instanceof FormSectionConf) {
//            return element.getId();
//        } else {
//            throw new IllegalStateException("Unknown element type: " + element.getClass());
//        }
//    }
//
//    // ----------------------- helpers ------------------------
//
//    private String getTextOrNull(JsonNode n) {
//        if (n == null || n.isNull()) return null;
//        return n.asText(null);
//    }
//
//    /**
//     * Safely traverse a JsonNode with dot-separated path (e.g. "a.b.c").
//     * Returns null if anything missing.
//     */
//    private JsonNode getNestedNode(JsonNode root, String dottedPath) {
//        if (root == null || root.isNull() || dottedPath == null || dottedPath.isEmpty()) return null;
//        String[] parts = dottedPath.split("\\.");
//        JsonNode cur = root;
//        for (String p : parts) {
//            if (cur == null || cur.isNull()) return null;
//            cur = cur.get(p);
//        }
//        return cur;
//    }
//}
//
//
