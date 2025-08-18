package org.nmcpye.datarun.jpa.etl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.nmcpye.datarun.jpa.etl.exception.InvalidCategoryException;
import org.nmcpye.datarun.jpa.etl.exception.MissingRepeatUidException;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.nmcpye.datarun.jpa.option.service.OptionService;
import org.nmcpye.datarun.security.SecurityUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@SuppressWarnings("unused")
public class SubmissionNormalizer {

    private final TemplateElementMap elementMap;
    private final OptionService optionService;
    private final boolean generateMissingRepeatUids;

    public SubmissionNormalizer(OptionService optionService,
                                TemplateElementMap elementMap,
                                boolean generateMissingRepeatUids) {
        this.elementMap = elementMap;
        this.optionService = optionService;
        this.generateMissingRepeatUids = generateMissingRepeatUids;
    }

    public NormalizedSubmission normalize(DataSubmission submission) {
        Object formData = submission.getFormData();
        Map<String, Object> map = new ObjectMapper().convertValue(formData, new TypeReference<Map<String, Object>>() {
        });

        final var currentUser = SecurityUtils.getCurrentUserDetailsOrNull();
        final NormalizedSubmission ns = new NormalizedSubmission(
            submission.getUid(),
            submission.getForm(),
            submission.getAssignment()
        );

        // walk root
        extractNode(/*submission.getFormData()*/ map, /*currentPath*/ null,
            ns, submission, /*repeatId*/ null, /*repeatIndex*/ null, /*categoryId*/ null);

        return ns;
    }

    @SuppressWarnings("unchecked")
    private void extractNode(
        Map<String, Object> node,
        String currentPath,
        NormalizedSubmission ns,
        DataSubmission submission,
        String currentRepeatId,
        Long currentRepeatIndex,
        String currentCategoryId) {

        if (node == null) return;

        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();
            String childPath = (currentPath == null || currentPath.isEmpty()) ? key : currentPath + "." + key;

            AbstractElement element = elementMap.getElementNamePathMapCache().get(childPath);
            if (element == null) {
                continue;
            }

            if (element instanceof FormSectionConf &&
                Boolean.TRUE.equals(((FormSectionConf) element).getRepeatable()) &&
                rawValue instanceof List) {

                List<Map<String, Object>> items = (List<Map<String, Object>>) rawValue;
                long idx = 0L;
                for (Map<String, Object> item : items) {
                    Object uidObj = item.get("_uid");
                    String repeatId = uidObj != null ? uidObj.toString() : null;

                    if (repeatId == null) {
                        if (generateMissingRepeatUids) {
                            repeatId = CodeGenerator.nextUlid();
                            item.put("_uid", repeatId); // optionally propagate back
                        } else {
                            throw new MissingRepeatUidException(List.of(
                                new MissingRepeatUidException.MissingRepeatUid(childPath, (int) idx)
                            ));
                        }
                    }

                    RepeatInstance ri = RepeatInstance.builder()
                        .id(repeatId)
                        .submission(ns.getSubmissionId())
                        .repeatPath(childPath)
                        .repeatIndex(idx)
                        .clientUpdatedAt(submission.getFinishedEntryTime())
                        .createdDate(submission.getCreatedDate())
                        .lastModifiedDate(submission.getLastModifiedDate())
                        // temporarily string for the test to work, because user info comes from the spring contexts,
                        .createdBy(submission.getCreatedBy())
                        .lastModifiedBy(submission.getLastModifiedBy())
                        .build();

                    ns.addRepeatInstance(ri);
                    ns.addIncomingUid(childPath, repeatId);

                    String categoryValue = extractCategoryIdForItem(item, childPath);

                    extractNode(item, childPath, ns, submission, repeatId, idx, categoryValue);
                    idx++;
                }
            } else if (rawValue instanceof Map) {
                extractNode((Map<String, Object>) rawValue, childPath, ns, submission,
                    currentRepeatId, currentRepeatIndex, currentCategoryId);
            } else if (rawValue instanceof List<?> list && element instanceof FormDataElementConf field) {
                String elementId = field.getId();
                String optionSetId = field.getOptionSet(); // may be null

                if (list.isEmpty()) {
                    ns.markMultiSelectExplicitlyEmpty(currentRepeatId, elementId);
                    continue;
                }

                // Normalize codes
                List<String> codes = list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .toList();

                // Map codes -> option ids (throws InvalidOptionCodesException if any missing)
                Map<String, String> codeToOptionId = optionService.validateAndMapOptionCodes(codes, optionSetId);

                for (String code : codes) {
                    String optionId = codeToOptionId.get(code);
                    ElementDataValue row = ElementDataValue.builder()
                        .element(elementId)
                        .submission(ns.getSubmissionId())
                        .repeatInstance(currentRepeatId)
                        .category(currentCategoryId)
                        .assignment(ns.getAssignmentId())
                        .template(ns.getTemplateId())
                        .option(optionId)           // option id is canonical identity for multi-select
                        .valueText(code)            // keep the original code/text as denormalized label (optional)
                        .createdDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
                        .lastModifiedDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
                        .build();

                    ns.addValueRow(row);
                    ns.addMultiSelectIdentity(currentRepeatId, elementId, optionId);
                }
            } else {
                // --- primitive (single-value e.g text / numeric / boolean) branch ---
                String elementId = resolveElementId(element);

                ElementDataValue row = ElementDataValue.builder()
                    .element(elementId)
                    .submission(ns.getSubmissionId())
                    .repeatInstance(currentRepeatId)
                    .category(currentCategoryId)
                    .assignment(ns.getAssignmentId())
                    .template(ns.getTemplateId())
                    .createdDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
                    .lastModifiedDate(submission.getLastModifiedDate() == null ? submission.getCreatedDate() : submission.getLastModifiedDate())
                    .build();

                // set typed values as best-effort: prefer to store primitive as valueText if not numeric/bool
                if (rawValue == null) {
                    row.setValueText(null);
                } else if (rawValue instanceof Number) {
                    row.setValueNumber(new java.math.BigDecimal(rawValue.toString()));
                } else if (rawValue instanceof Boolean) {
                    row.setValueBoolean((Boolean) rawValue);
                } else {
                    // fallback to string representation
                    row.setValueText(rawValue.toString());
                }

                ns.addValueRow(row);
            }
        }
    }

    private String extractCategoryIdForItem(Map<String, Object> item, String repeatPath) {
        String categoryElementId = elementMap.getRepeatCategoryElementMapCache().get(repeatPath);
        if (categoryElementId == null) return null;

        String categoryFullPath = elementMap.getFieldElementReversePathMap().get(categoryElementId);
        if (categoryFullPath == null) return null;
        String relative = categoryFullPath.startsWith(repeatPath + ".") ?
            categoryFullPath.substring(repeatPath.length() + 1) : categoryFullPath;
        Object val = item.get(relative);
        return val != null ? val.toString() : null;
    }

    private String resolveElementId(AbstractElement element) {
        if (element instanceof FormDataElementConf) {
            return element.getId();
        } else if (element instanceof FormSectionConf) {
            return element.getId(); // usually name()
        } else {
            throw new IllegalStateException("Unknown element type: " + element.getClass());
        }
    }

//    private void applyCategoryToSiblings(
//        RepeatInstance repeat,
//        String categoryValue) {
//
//        valuesInRepeat(repeat).forEach(value ->
//            value.setCategoryId(categoryValue));
//    }

    public void validateCategoryElement(FormDataElementConf element, Object vale) {
        if (!element.getType().isComplexReference()) {
            throw new InvalidCategoryException(element.getId(),
                element.getType().name());
        }
    }
}

