package org.nmcpye.datarun.jpa.etl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.nmcpye.datarun.jpa.etl.exception.InvalidCategoryValueException;
import org.nmcpye.datarun.jpa.etl.exception.MissingRepeatUidException;
import org.nmcpye.datarun.jpa.etl.model.CategoryResolutionResult;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.nmcpye.datarun.jpa.etl.util.CategoryResolver;
import org.nmcpye.datarun.jpa.etl.util.ElementValueResolver;
import org.nmcpye.datarun.jpa.etl.util.ReferenceResolver;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Normalizes a [DataSubmission] into a [NormalizedSubmission] (repeat instances + element rows).
///
/// @author Hamza Assada 19/08/2025 (7amza.it@gmail.com)
/// @see TemplateElementMap
/// @see CategoryResolver
/// @see EtlCoordinatorService
@Slf4j
@Service
@RequiredArgsConstructor
public class Normalizer {

    /// For mapping option codes and resolving categories
    private final CategoryResolver categoryResolver;
    private final ReferenceResolver referenceResolver;
    private final ElementValueResolver elementValueResolver;
    /// fetch a Cached template elements maps
    private final TemplateElementService templateElementService;
    private final ObjectMapper objectMapper;

    /// A small, immutable helper class to pass context down the recursion.
    ///
    /// @param instanceId The ID of the immediate repeat instance container. Null for the root.
    private record RepeatContext(String instanceId) {
        public static final RepeatContext ROOT = new RepeatContext(null);
    }

    /// Main entry point for normalization.
    /// Translates a [DataSubmission]'s data into a complete,
    /// self-contained [NormalizedSubmission]
    public NormalizedSubmission normalize(DataSubmission submission, boolean generateMissingRepeatUids) {
        final TemplateElementMap elementMap =
            templateElementService.getTemplateElementMap(submission.getForm(),
                submission.getFormVersion());

        final Map<String, Object> formData =
            objectMapper.convertValue(submission.getFormData(), new TypeReference<>() {
            });

        final NormalizedSubmission ns = new NormalizedSubmission(
            submission.getUid(),
            submission.getForm(),
            submission.getAssignment()
        );

        // Start the recursive traversal from the root of the form data.
        extractNode(formData, "", ns, submission, elementMap,
            RepeatContext.ROOT, generateMissingRepeatUids);

        return ns;
    }

    @SuppressWarnings("unchecked")
    private void extractNode(
        final Map<String, Object> node,
        final String currentPath,
        final NormalizedSubmission ns,
        final DataSubmission submission,
        final TemplateElementMap elementMap,
        final RepeatContext context,
        final boolean generateMissingRepeatUids
    ) {
        if (node == null) return;

        for (Map.Entry<String, Object> entry : node.entrySet()) {
            final String key = entry.getKey();
            final Object rawValue = entry.getValue();
            final String childPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            final AbstractElement element = elementMap.getElementByNamePathMap().get(childPath);
            final ElementTemplateConfig elementConf = elementMap.getElementConfigByNamePathMap().get(childPath);

            if (element == null) continue; // Skip data not defined in the template

            // --- REPEATABLE SECTION ---
            if (element instanceof FormSectionConf section && section.getRepeatable() &&
                rawValue instanceof List<?> items) {
                long idx = 0L;
                for (Object itemObj : items) {
                    if (!(itemObj instanceof Map)) continue; // Skip malformed items
                    Map<String, Object> item = (Map<String, Object>) itemObj;

                    // Check for the soft-delete flag from the client
                    boolean isDeleted = item.containsKey("_deleted") && Boolean.TRUE.equals(item.get("_deleted"));

                    String repeatId = getOrGenerateRepeatId(item, childPath,
                        idx, generateMissingRepeatUids);

                    // Resolve category for this new repeat item.
                    CategoryResolutionResult category = extractCategoryForItem(item, childPath, elementMap);

                    // Create the RepeatInstance DTO, linking it to its parent via the context.
                    final var ri = RepeatInstance.builder()
                        .id(repeatId)
                        .submissionId(ns.getSubmissionId())
                        .categoryId(category != null ? category.getId() : null)
                        .categoryKind(category != null ? category.getKind() : null)
                        .categoryName(category != null ? category.getName() : null)
                        .categoryLabel(toStringLabel(category != null ? category.getLabel() : null))
                        .repeatPath(childPath)
                        .parentRepeatInstanceId(context.instanceId()) // NESTED HIERARCHY LINK
                        .repeatIndex(idx)
                        .repeatSectionLabel(toStringLabel(section.getLabel()))
                        .submissionCompletedAt(submission.getFinishedEntryTime())
                        .clientUpdatedAt(submission.getLastModifiedDate())
                        .createdDate(Instant.now())
                        .lastModifiedDate(Instant.now())
                        .createdBy(submission.getCreatedBy());

                    if (isDeleted) {
                        // If the client marks it as deleted, persist that intent.
                        // Use a client-provided timestamp if available, otherwise use now().
                        Instant deletedAt = Instant.now();
                        try {
                            Object deletedAtObj = item.get("_deleted_at");
                            deletedAt = (deletedAtObj instanceof String)
                                ? Instant.parse((String) deletedAtObj) : Instant.now();
                        } catch (DateTimeParseException e) {
                            log.error("Error parsing deleted_at of a repeat instance {} client marked as deleted.", repeatId);
                        }

                        ri.deletedAt(deletedAt);
                    }
                    // ------------------------------------
                    ns.addRepeatInstance(ri.build());

                    // If an instance is marked as deleted, we should NOT process its children.
                    if (isDeleted) {
                        idx++;
                        continue; // Skip to the next repeat item
                    }

                    RepeatContext newContext = new RepeatContext(repeatId);

                    // Recurse into the nested item with the new context.
                    extractNode(item, childPath, ns, submission, elementMap, newContext, generateMissingRepeatUids);
                    idx++;
                }
            }
            // --- NESTED, NON-REPEATABLE SECTION ---
            else if (element instanceof FormSectionConf && rawValue instanceof Map) {
                // Recurse into the section, passing the same context down.
                extractNode((Map<String, Object>) rawValue, childPath, ns, submission, elementMap, context, generateMissingRepeatUids);
            }
            // --- MULTI-SELECT ELEMENT ---
            else if (element instanceof FormDataElementConf field && rawValue instanceof List<?> list) {
                if (list.isEmpty()) continue; // With Purge/Replace, we simply do nothing for empty lists.

                List<String> codes = list.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim).toList();
                Map<String, String> codeToOptionId = referenceResolver.mapOptionCodesToIds(codes, field.getOptionSet());

                for (String code : codes) {
                    ns.addValueRow(buildElementDataValue(submission, field, context,
                        codeToOptionId.get(code), code, elementConf.getId()));
                }
            }
            // --- PRIMITIVE SINGLE-VALUE ELEMENT ---
            else if (element instanceof FormDataElementConf field) {
                ns.addValueRow(buildElementDataValue(submission, field, context, null, rawValue, elementConf.getId()));
            }
        }
    }

    /// Centralized builder for ElementDataValue to ensure consistency and align with the new, leaner DTO.
    private ElementDataValue buildElementDataValue(DataSubmission submission,
                                                   FormDataElementConf field,
                                                   RepeatContext context,
                                                   String optionId, Object rawValue, Long elementConfId) {
        // --- try parse value based on valueType
        ElementDataValue.ElementDataValueBuilder builder =
            elementValueResolver.resolveValue(rawValue, field);

        // if single select validate option

        // --- CORE IDENTIFIERS & DIMENSIONS ---
        builder.submissionId(submission.getUid())
            .elementId(field.getId())
            // --- EXPANDED ASSIGNMENT DIMENSIONS ---
            .assignmentId(submission.getAssignment())
            .teamId(submission.getTeam())
            .orgUnitId(submission.getOrgUnit())
            .activityId(submission.getActivity())
            .elementConfigId(elementConfId)
            // --- CONTEXT FROM REPEAT ---
            .repeatInstanceId(context.instanceId())
            // --- VALUE ---
            .optionId(optionId) // Set for multi-select, null otherwise
            // --- METADATA ---
            .elementLabel(toStringLabel(field.getLabel()))
            .lastModifiedDate(Instant.now())
            .createdDate(Instant.now());

        return builder.build();
    }

    private String getOrGenerateRepeatId(Map<String, Object> item, String path, long index, boolean generateMissing) {
        Object uidObj = item.get("_id");
        if (uidObj != null && !uidObj.toString().isBlank()) {
            return uidObj.toString();
        }
        if (generateMissing) {
            String generatedId = CodeGenerator.nextUlid();
            item.put("_id", generatedId); // Propagate back for potential client use
            return generatedId;
        }
        throw new MissingRepeatUidException(List.of(new MissingRepeatUidException.MissingRepeatUid(path, (int) index)));
    }

    private String toStringLabel(Map<String, String> label) {
        String labelString = null;
        try {
            labelString = objectMapper.writeValueAsString(label);
        } catch (Exception ex) {
            log.warn("could not serialize element label into json string for template field table: {}, {}",
                label, label);
        }

        return labelString;
    }

    /// Resolve category for an item given the repeatPath.
    /// Returns null if there is no category configured for this repeat.
    private CategoryResolutionResult extractCategoryForItem(Map<String, Object> item, String repeatPath,
                                                            TemplateElementMap elementMap) {
        String categoryElementId = elementMap.getRepeatPathToCategoryElementIdMap().get(repeatPath);
        if (categoryElementId == null) return null;

        // find element full path from element id -> Element
        FormDataElementConf categoryFullPath = elementMap.getFieldElementReverseIdPathMap().get(categoryElementId);
        if (categoryFullPath == null) {
            log.warn("Template metadata: category element id {} has no reverse path", categoryElementId);
            return null;
        }
        // we use name as a key, and category is available as an id
        String categoryKey = categoryFullPath.getName();

        Object rawVal = item.get(categoryKey);
        if (rawVal == null) return null;

        // delegate to resolver which may try id->lookup or code->lookup based on field type
        try {
            return categoryResolver.resolveCategory(rawVal, categoryFullPath);
        } catch (InvalidCategoryValueException ex) {
            // policy: treat as hard failure so the submission doesn't silently store wrong category
            throw ex;
        } catch (Throwable t) {
            log.error("Error resolving category for repeatPath={} rawVal={} : {}", repeatPath, rawVal, t.getMessage(), t);
            throw new InvalidCategoryValueException("Failed to resolve category for repeat " + repeatPath + ": " + t.getMessage(), t);
        }
    }
}

