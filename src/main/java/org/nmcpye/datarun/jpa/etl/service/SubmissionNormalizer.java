package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
import org.nmcpye.datarun.jpa.etl.exception.MissingRepeatUidException;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.nmcpye.datarun.jpa.option.service.OptionService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
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
    /**
     * config: true->generate server-side, false->throw
     */
    private final boolean generateMissingRepeatUids;

    public SubmissionNormalizer(OptionService optionService, TemplateElementMap elementMap, boolean generateMissingRepeatUids) {
        this.elementMap = elementMap;
        this.optionService = optionService;
        this.generateMissingRepeatUids = generateMissingRepeatUids;
    }

    public NormalizedSubmission normalize(DataFormSubmission submission) {
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrNull();
        final NormalizedSubmission ns = new NormalizedSubmission(
            submission.getUid(),
            submission.getForm(),
            submission.getAssignment());

        // walk root
        extractNodeHandleMultiStrategy(submission.getFormData(), /*currentPath*/ null,
            ns, submission, /*repeatId*/ null, /*repeatIndex*/ null, /*categoryId*/ null);

        return ns;
    }

    @SuppressWarnings("unchecked")
    private void extractNodeHandleMultiStrategy(
        Map<String, Object> node,
        String currentPath,
        NormalizedSubmission ns,
        DataFormSubmission submission,
        String currentRepeatId,
        Long currentRepeatIndex,
        String currentCategoryId) {

        if (node == null) return;

        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();
            String childPath = (currentPath == null || currentPath.isEmpty()) ? key : currentPath + "." + key;

            AbstractElement element = elementMap.getElementPathMapCache().get(childPath);
            if (element == null) {
                // Not part of template -> skip
                continue;
            }

            // Repeatable section
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
                            repeatId = CodeGenerator.ULIDGenerator.nextString();
                            item.put("_uid", repeatId); // optionally propagate back
                        } else {
                            throw new MissingRepeatUidException(List.of(new MissingRepeatUidException.MissingRepeatUid(childPath, (int) idx)));
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
                        .createdBy("SecurityUtils.getCurrentUserLoginOrThrow()")
                        .lastModifiedBy("SecurityUtils.getCurrentUserLoginOrThrow()")
                        .build();

                    ns.addRepeatInstance(ri);
                    ns.addIncomingUid(childPath, repeatId);

                    // extract category for this repeat if configured
                    String categoryValue = extractCategoryIdForItem(item, childPath);

                    // Recurse into this repeat item with repeatId context
                    extractNodeHandleMultiStrategy(item, childPath, ns, submission, repeatId,idx,categoryValue);
                    idx++;
                }
            } else if (rawValue instanceof Map) {
                // nested non-repeat
                extractNodeHandleMultiStrategy((Map<String, Object>) rawValue, childPath, ns, submission,
                    currentRepeatId, currentRepeatIndex, currentCategoryId);
            } else if (rawValue instanceof List<?> list && element instanceof FormDataElementConf field) {
                // list-of-primitives (multi-select). Must emit one value row per entry,
                // and record incoming identities (optionId OR valueText).
                String elementId = field.getId();
                String optionSetId = field.getOptionSet(); // may be null

                // If the list is explicitly empty, record presence as empty (so persister can clear stored selections)
                if (list.isEmpty()) {
                    ns.markMultiSelectExplicitlyEmpty(currentRepeatId, elementId);
                    continue;
                }

                // Convert items to normalized codes (strings), trim them
                List<String> codes = list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .toList();

                // Validate & map codes -> optionId using OptionService (throws InvalidOptionCodesException if missing)
                Map<String, String> codeToOptionId = optionService.validateAndMapOptionCodes(codes, optionSetId);

                // For each code, create a SubmissionValueRow with optionId set (use optionId as identity)
                for (String code : codes) {
                    String optionId = codeToOptionId.get(code); // should be present, otherwise service would throw

                    SubmissionValueRow row = SubmissionValueRow.builder()
                        .element(elementId)
                        .submission(ns.getSubmissionId())
                        .repeatInstance(currentRepeatId) // see note below
                        .category(currentCategoryId)
                        .assignment(ns.getAssignmentId())
                        .template(ns.getTemplateId())
                        .option(optionId)
                        .lastModifiedDate(submission.getLastModifiedDate())
                        .createdDate(submission.getLastModifiedDate())
                        .build();

                    row.setValue(code); // optionally keep label, but we prefer optionId

                    ns.addValueRow(row);

                    // record selection identity used for mark-and-sweep
                    ns.addMultiSelectIdentity(currentRepeatId, elementId, optionId);
                }
            } else {
                // primitive: single value (text/num/bool) -> store single row
                String elementId = resolveElementId(element);
                SubmissionValueRow row = SubmissionValueRow.builder()
                    .element(elementId)
                    .submission(ns.getSubmissionId())
                    .repeatInstance(currentRepeatId) // see note below
                    .category(currentCategoryId)
                    .assignment(ns.getAssignmentId())
                    .template(ns.getTemplateId())
//                    .deletedAt(submission.getDeletedAt())
                    .lastModifiedDate(submission.getLastModifiedDate())
                    .createdDate(submission.getLastModifiedDate())
                    .build();

                row.setValue(rawValue);

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

    @SuppressWarnings("unchecked")
    private void extractNode(
        Map<String, Object> node,
        String currentPath,
        NormalizedSubmission ns,
        DataFormSubmission submission,
        String currentRepeatId,
        Long currentRepeatIndex,
        String currentCategoryId) {

        if (node == null) return;

        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();
            String childPath = (currentPath == null || currentPath.isEmpty()) ? key : currentPath + "." + key;

            AbstractElement element = elementMap.getElementPathMapCache().get(childPath);

            if (element == null) {
                // not part of template -> skip
                continue;
            }

            // repeatable section
            if (element instanceof FormSectionConf sectionConf &&
                Boolean.TRUE.equals(sectionConf.getRepeatable()) &&
                rawValue instanceof List) {

                List<Map<String, Object>> items = (List<Map<String, Object>>) rawValue;
                long idx = 0L;
                for (Map<String, Object> item : items) {
                    Object uidObj = item.get("_uid");
                    String repeatUid = uidObj != null ? uidObj.toString() : null;

                    if (repeatUid == null) {
                        if (generateMissingRepeatUids) {
                            repeatUid = CodeGenerator.ULIDGenerator.nextString();

                            // optionally put it back into item for persisting it back to client later
                            item.put("_uid", repeatUid);
                        } else {
                            throw new MissingRepeatUidException(List.of(new MissingRepeatUidException.MissingRepeatUid(childPath, (int) idx)));
                        }
                    }

                    // create RepeatInstance candidate (clientUpdatedDate is read from submission)
                    RepeatInstance ri = RepeatInstance.builder()
                        .id(repeatUid)
                        .submission(ns.getSubmissionId())
                        .repeatPath(childPath)
                        .repeatIndex(idx)
                        .createdBy(submission.getCreatedBy())
                        .lastModifiedBy(submission.getLastModifiedBy())
                        .clientUpdatedAt(submission.getFinishedEntryTime())
                        .lastModifiedDate(submission.getLastModifiedDate())
                        .createdDate(submission.getLastModifiedDate())
                        .build();
                    ns.addRepeatInstance(ri);
                    ns.addIncomingUid(childPath, repeatUid);

                    // extract category id (if configured)
                    String categoryId = extractCategoryIdForItem(item, childPath);

                    // recurse inside repeat item
                    extractNode(item, childPath, ns, submission, repeatUid, idx, categoryId);
                    idx++;
                }
            } else if (rawValue instanceof Map) {
                // nested non-repeat
                extractNode((Map<String, Object>) rawValue, childPath, ns, submission, currentRepeatId, currentRepeatIndex, currentCategoryId);
            } else {
                // primitive: convert to SubmissionValueRow builder
                String elementId = resolveElementId(element);
                SubmissionValueRow row = SubmissionValueRow.builder()
                    .element(elementId)
                    .submission(ns.getSubmissionId())
                    .repeatInstance(currentRepeatId) // see note below
                    .category(currentCategoryId)
                    .assignment(ns.getAssignmentId())
                    .template(ns.getTemplateId())
                    .lastModifiedDate(submission.getLastModifiedDate())
                    .createdDate(submission.getLastModifiedDate())
                    .build();

                row.setValue(rawValue);
                // Note: we set repeatId by looking at context: the extractor passes currentRepeatIndex and also
                // when recursing into a repeatable item we have added the repeatInstance and added its uid into ns.incomingRepeatUids.
                // To avoid complex "context stack", we passed the repeatId via a slightly different mechanism above: the recursion uses
                // item context; in this implementation we should pass the repeatUid explicitly instead of repeatIndex.

                ns.addValueRow(row);
            }
        }
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



}

