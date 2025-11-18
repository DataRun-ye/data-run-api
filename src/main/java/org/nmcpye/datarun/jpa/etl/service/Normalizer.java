package org.nmcpye.datarun.jpa.etl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.etl.dto.OutboxDto;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.nmcpye.datarun.jpa.etl.exception.InvalidCategoryValueException;
import org.nmcpye.datarun.jpa.etl.exception.MissingRepeatUidException;
import org.nmcpye.datarun.jpa.etl.model.CategoryResolutionResult;
import org.nmcpye.datarun.jpa.etl.model.NormalizedOutboxSubmission;
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
import java.util.UUID;

/**
 * Normalizer — converts a raw DataSubmission into a self-contained NormalizedSubmission.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Traverse form JSON (including nested and repeat sections)</li>
 *   <li>Produce RepeatInstance DTOs for repeat sections</li>
 *   <li>Produce ElementDataValue rows for primitive and multi-select fields</li>
 *   <li>Delegate lookups and conversions to injected resolvers/services</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Purely procedural: recursion is used to walk the JSON tree.</li>
 *   <li>Repeat instance IDs are either read-from-client or generated (ULID).</li>
 *   <li>Category resolution is delegated and failures are treated as hard errors.</li>
 * </ul>
 *
 * @author Hamza Assada (7amza.it@gmail.com)
 * @see TemplateElementMap
 * @see CategoryResolver
 * @see EtlCoordinatorService
 * @since 19/08/2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Normalizer {

    /* ----------------------------
       Dependencies (injected)
       ---------------------------- */

    /**
     * Maps category values and resolves category metadata for repeat items.
     */
    private final CategoryResolver categoryResolver;

    /**
     * Resolves references (option code → option id, etc).
     */
    private final ReferenceResolver referenceResolver;

    /**
     * Converts raw/JSON values into ElementDataValue builders based on field type.
     */
    private final ElementValueResolver elementValueResolver;

    /**
     * Provides cached TemplateElementMap (template metadata lookups).
     */
    private final TemplateElementService templateElementService;

    private final DataSubmissionRepository submissionRepo;

    /**
     * Jackson mapper used for converting form-data and serializing labels.
     */
    private final ObjectMapper objectMapper;

    /**
     * Context object passed down recursion to track the current repeat instance id.
     *
     * @param instanceId id of the immediate parent repeat instance, {@code null} for root
     */
    private record RepeatContext(String instanceId) {
        public static final RepeatContext ROOT = new RepeatContext(null);
    }

    /* ----------------------------
       Public API
       ---------------------------- */

    /**
     * Normalize a DataSubmission into a NormalizedSubmission.
     *
     * <p>This is the main entry point: it fetches the template map, converts the raw form-data to a
     * Map, creates the NormalizedSubmission wrapper, and starts the recursive extraction.
     * @deprecated use normalizeOutbox
     * @param submission                the incoming submission payload
     * @param generateMissingRepeatUids when true, generate ULIDs for repeat items missing client IDs
     * @return a fully populated NormalizedSubmission containing repeat instances and value rows
     */
    @Deprecated
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

    public NormalizedSubmission normalizeOutbox(OutboxDto outbox, UUID ingestId) {
        DataSubmission submission = submissionRepo.findById(outbox.getSubmissionId())
            .orElseThrow(() -> new IllegalStateException("Submission not found: " + outbox.getSubmissionId()));
        final TemplateElementMap elementMap =
            templateElementService.getTemplateElementMap(submission.getForm(),
                submission.getFormVersion());

        final Map<String, Object> formData =
            objectMapper.convertValue(submission.getFormData(), new TypeReference<>() {
            });

        final NormalizedOutboxSubmission ns = new NormalizedOutboxSubmission(
            submission.getUid(),
            submission.getForm(),
            submission.getAssignment(),
            ingestId,
            outbox.getOutboxId()
        );

        // Start the recursive traversal from the root of the form data.
        extractNode(formData, "", ns, submission, elementMap,
            RepeatContext.ROOT, true);

        return ns;
    }

    /* ----------------------------
    Internal helpers (signatures only)
    ---------------------------- */

    /**
     * Recursive traversal of a node within the form-data tree.
     *
     * <p>Behaviors:
     * <ul>
     *   <li>When encountering a repeatable section: create RepeatInstance(s), optionally mark deleted, and recurse into items.</li>
     *   <li>When encountering a nested non-repeat section: recurse normally.</li>
     *   <li>When encountering a multi-select: map codes → option ids and emit a row per selected option.</li>
     *   <li>When encountering a primitive field: build a single ElementDataValue row.</li>
     * </ul>
     *
     * @param node                      current node map (may be null)
     * @param currentPath               dot-delimited path from root to this node
     * @param ns                        accumulating NormalizedSubmission
     * @param submission                original submission metadata
     * @param elementMap                template metadata helper
     * @param context                   current repeat context (parent repeat instance id)
     * @param generateMissingRepeatUids whether to generate ULIDs for missing repeat ids
     */
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
            final AbstractElement element = elementMap.getElementByJsonDataPathMap().get(childPath);
            final TemplateElement etc = elementMap.getElementConfigByNamePathMap().get(childPath);

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

//                    // Resolve category for this new repeat item.
//                    CategoryResolutionResult category = extractCategoryForItem(item, childPath, elementMap);

                    // Create the RepeatInstance DTO, linking it to its parent via the context.
                    final var ri = RepeatInstance.builder()
                        .id(repeatId)
//                        .teUid(etc.getUid())
                        .canonicalElementUid(etc.getCanonicalElementUid())
                        .submissionUid(ns.getSubmissionUid())
                        .repeatPath(childPath)
                        .canonicalPath(etc.getCanonicalPath())
                        .parentRepeatInstanceId(context.instanceId()) // NESTED HIERARCHY LINK
                        .repeatIndex(idx)
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
                Map<String, String> codeToOptionUid = referenceResolver.mapOptionCodesToUids(codes, field.getOptionSet());

                for (String code : codes) {
                    ns.addValueRow(buildElementDataValue(submission, field, context,
                        codeToOptionUid.get(code), code, etc));
                }
            }
            // --- PRIMITIVE SINGLE-VALUE ELEMENT ---
            else if (element instanceof FormDataElementConf field) {
                ns.addValueRow(buildElementDataValue(submission, field, context, null,
                    rawValue, etc));
            }
        }
    }

    /// Centralized builder for ElementDataValue to ensure consistency and align with the new, leaner DTO.
    private ElementDataValue buildElementDataValue(DataSubmission submission,
                                                   FormDataElementConf field,
                                                   RepeatContext context,
                                                   String optionUid, Object rawValue,
                                                   TemplateElement etc) {
        // --- try parse value based on dataType
        ElementDataValue.ElementDataValueBuilder builder =
            elementValueResolver.resolveValue(rawValue, field);

        // if single select validate option

        // --- CORE IDENTIFIERS & DIMENSIONS ---
        builder.submissionUid(submission.getUid())
            .canonicalElementUid(etc.getCanonicalElementUid())
            // --- EXPANDED ASSIGNMENT DIMENSIONS ---
            .assignmentUid(submission.getAssignment())
            .teamUid(submission.getTeam())
            .orgUnitUid(submission.getOrgUnit())
            .activityUid(submission.getActivity())
//            .teUid(etc.getUid())
            .canonicalPath(etc.getCanonicalPath())
            // --- CONTEXT FROM REPEAT ---
            .repeatInstanceId(context.instanceId())
            // --- VALUE ---
            .optionUid(optionUid) // Set for multi-select, null otherwise
            // --- METADATA ---
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

