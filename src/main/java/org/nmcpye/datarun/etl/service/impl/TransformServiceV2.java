package org.nmcpye.datarun.etl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.etl.dto.CanonicalElementAnchorDto;
import org.nmcpye.datarun.etl.dto.EventDto;
import org.nmcpye.datarun.etl.dto.RefTypeValue;
import org.nmcpye.datarun.etl.model.SubmissionContext;
import org.nmcpye.datarun.etl.model.TallCanonicalRow;
import org.nmcpye.datarun.etl.model.TemplateContext;
import org.nmcpye.datarun.etl.repository.EventEntityJdbcRepository;
import org.nmcpye.datarun.etl.repository.RefTypeValueRepository;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;
import org.nmcpye.datarun.utils.UuidUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Lightweight wrapper: reuses TransformServiceRobust to produce TallCanonicalRow list,
 * then resolves ref-values, sets valueRefUid on rows, computes anchors per instance_key,
 * upserts event rows and returns enriched tall rows ready for persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransformServiceV2 {

    private final TransformServiceRobust baseTransform;
    private final RefTypeValueResolutionService refTypeValueResolutionService;
    private final RefTypeValueRepository refTypeValueRepository;
    private final EventEntityJdbcRepository eventJdbcRepository;

    public static class ResolutionCandidate {
        public final RefTypeValue refTypeValue;
        public final double confidence;
        public final Instant resolvedAt;
        public final int priority;

        public ResolutionCandidate(RefTypeValue refTypeValue,
                                   double confidence, Instant resolvedAt, int priority) {
            this.refTypeValue = refTypeValue;
            this.confidence = confidence;
            this.resolvedAt = resolvedAt;
            this.priority = priority;
        }
    }

    /**
     * Transform + resolve + upsert events.
     *
     * @param root                   JSON payload
     * @param templateContext        template elements and anchors maps
     * @param flattenPrimitiveArrays flatten flag forwarded to base transform
     * @param submissionContext      context map with keys: submissionUid, submissionId, templateUid, templateVersionUid, submissionCreationTime, startTime, assignmentUid, activityUid, orgUnitUid, teamUid
     */
    public List<TallCanonicalRow> transform(JsonNode root,
                                            boolean flattenPrimitiveArrays,
                                            SubmissionContext submissionContext,
                                            TemplateContext templateContext) {

//        String submissionUid = submissionContext.getSubmissionUid();
//        Long submissionSerial = submissionContext.getSubmissionSerial();
//        String submissionId = submissionContext.getSubmissionId();
//        String templateUid = submissionContext.getTemplateUid();
//        String templateVersionUid = submissionContext.getTemplateVersionUid();
//        String activityUid = submissionContext.getActivityUid();
//        String orgUnitUid = submissionContext.getOrgUnitUid();
//        String teamUid = submissionContext.getTeamUid();
//        String assignmentUid = submissionContext.getAssignmentUid();
//        Instant submissionCreationTime = submissionContext.getSubmissionCreationTime();
//        Instant startTime = submissionContext.getStartTime();

        log.debug("TransformV2: submissionUid={}, templateUid={}, templateVersionUid={}, payloadNull={}",
            submissionContext.getSubmissionUid(), submissionContext.getTemplateUid(),
            submissionContext.getTemplateVersionUid(), root == null);

        List<TallCanonicalRow> rows = baseTransform.transform(
            root,
            templateContext.allCanonicalElementsMap().values().stream().toList(),
            flattenPrimitiveArrays
        );

        if (rows == null || rows.isEmpty()) {
            log.debug("TransformV2: baseTransform returned no rows for submissionUid={}",
                submissionContext.getSubmissionUid());
            return Collections.emptyList();
        }

        // collectors
        Map<String, List<ResolutionCandidate>> candidatesByInstance = new HashMap<>();
        Set<String> allInstanceKeys = new HashSet<>();
        Map<UUID, RefTypeValue> refTypeValues = new HashMap<>();
        Map<String, String> instanceRepeatCeMap = new HashMap<>();
        Map<String, String> parentForInstance = new HashMap<>();

        // 1) scan rows: provenance, record repeat CE, resolve refs, collect candidates, capture parent from row
        for (TallCanonicalRow r : rows) {
            // provenance
            r.setSubmissionUid(submissionContext.getSubmissionUid());
            r.setSubmissionSerialNumber(submissionContext.getSubmissionSerial());
            r.setSubmissionId(submissionContext.getSubmissionId());
            r.setTemplateUid(submissionContext.getTemplateUid());
            r.setTemplateVersionUid(submissionContext.getTemplateUid());
            r.setAssignment(submissionContext.getAssignmentUid());
            r.setActivity(submissionContext.getActivityUid());
            r.setOrgUnit(submissionContext.getOrgUnitUid());
            r.setTeam(submissionContext.getTeamUid());
            r.setCreatedBy(submissionContext.getCreatedBy());
            r.setLastModifiedBy(submissionContext.getLastModifiedBy());

            String instanceKey = r.getRepeatInstanceId() != null ? r.getRepeatInstanceId() :
                submissionContext.getSubmissionUid();
            allInstanceKeys.add(instanceKey);

            // If a row declares its parent instance, capture it directly
            String explicitParent = r.getParentInstanceId(); // direct call, assumed present on TallCanonicalRow
            if (explicitParent != null) parentForInstance.put(instanceKey, explicitParent);

            String ceId = r.getCanonicalElementId();
            CanonicalElementAnchorDto anchorDto = templateContext.anchorsMap().get(ceId);
            CanonicalElement ceMeta = templateContext.allCanonicalElementsMap().get(ceId);
            SemanticType semanticType = ceMeta == null ? null : ceMeta.getSemanticType();
            String optionSetUid = ceMeta == null ? null : ceMeta.getOptionSetUid();
            boolean anchorAllowed = anchorDto != null && Boolean.TRUE.equals(anchorDto.getAnchorAllowed());
            int anchorPriority = anchorDto == null ? 100 : anchorDto.getAnchorPriority();

            // record repeat CE for the instance
            if (semanticType != null && semanticType.isRepeat()) {
                instanceRepeatCeMap.putIfAbsent(instanceKey, ceId);
            }

            // if ref-type, resolve and add candidate if anchorAllowed
            if (semanticType != null && semanticType.isRef()) {
                String token = r.getValueText();
                String refType = semanticType.name();
                String resolvedUid = refTypeValueResolutionService.resolve(token, refType, optionSetUid,
                    submissionContext.getActivityUid());

                RefTypeValue refTypeValue = RefTypeValue.builder()
                    .valueRefUid(resolvedUid)
                    .ceId(UuidUtils.toUuidOrNull(ceId))
                    .submissionUid(submissionContext.getSubmissionUid())
                    .instanceKey(instanceKey)
                    .templateUid(submissionContext.getTemplateUid())
                    .rawValue(token)
                    .refType(refType)
                    .createdAt(submissionContext.getStartTime())
                    .optionSetUid(optionSetUid)
                    .build();

                r.setValueRefUid(resolvedUid);

                if (token == null && r.getValueJson() != null) {
                    refTypeValue.setRawValue(r.getValueJson());
                }

                r.setValueRefType(refType);


                if (refTypeValue.getCeId() != null) {
                    refTypeValues.putIfAbsent(refTypeValue.getCeId(), refTypeValue);
                }

                if (anchorAllowed) {
                    ResolutionCandidate cand = new ResolutionCandidate(
                        refTypeValue,
                        refTypeValue.getValueRefUid() == null ? 0.0 : 1.0,
                        Instant.now(),
                        anchorPriority
                    );
                    candidatesByInstance.computeIfAbsent(instanceKey, k -> new ArrayList<>()).add(cand);
                }
            }
        }

        // 2) persist ref-type values in batch
        if (!refTypeValues.isEmpty()) {
            refTypeValueRepository.batchUpsert(refTypeValues.values());
        }

        // 3) Build events: use explicit parent when present, else for non-root fall back to submissionUid
        List<EventDto> eventsToPatchUpsert = new ArrayList<>(allInstanceKeys.size());
        Instant now = Instant.now();

        for (String instanceKey : allInstanceKeys) {
            boolean isRoot = instanceKey.equals(submissionContext.getSubmissionUid());
            String parentInstance = parentForInstance.get(instanceKey);
            String parentEventId = isRoot ? null : (parentInstance != null ? parentInstance :
                submissionContext.getSubmissionUid());

            // pick best anchor candidate for this instance
            List<ResolutionCandidate> candidates = candidatesByInstance.getOrDefault(instanceKey, Collections.emptyList());
            ResolutionCandidate best = null;
            if (!candidates.isEmpty()) {
                candidates.sort(Comparator.comparingInt((ResolutionCandidate c) -> c.priority)
                    .thenComparingDouble((ResolutionCandidate c) -> -c.confidence)
                    .thenComparing((ResolutionCandidate c) -> c.resolvedAt == null ? Instant.EPOCH : c.resolvedAt, Comparator.reverseOrder())
                    .thenComparing(c -> c.refTypeValue.getCeId() == null ? null : c.refTypeValue.getCeId().toString(), Comparator.nullsLast(Comparator.naturalOrder())));
                best = candidates.get(0);
            }

            String eventType = isRoot ? "root" : "repeat";
            String eventCeId = isRoot ? null : instanceRepeatCeMap.get(instanceKey);

            String anchorCeId = best != null && best.refTypeValue.getCeId() != null ? best.refTypeValue.getCeId().toString() : null;
            String anchorRefUid = best != null ? best.refTypeValue.getValueRefUid() : null;
            String anchorRefType = best != null ? best.refTypeValue.getRefType() : null;
            String anchorValueText = best != null ? best.refTypeValue.getRawValue() : null;
            BigDecimal anchorConfidence = best != null ? BigDecimal.valueOf(best.confidence) : null;
            Instant anchorResolvedAt = best != null ? best.resolvedAt : null;

            EventDto evt = EventDto.builder()
                .eventId(instanceKey)
                .assignmentUid(submissionContext.getAssignmentUid())
                .eventType(eventType)
                .parentEventId(parentEventId)
                .eventCeId(eventCeId)
                .createdBy(submissionContext.getCreatedBy())
                .lastModifiedBy(submissionContext.getLastModifiedBy())
                .submissionUid(submissionContext.getSubmissionUid())
                .submissionId(submissionContext.getSubmissionId())
                .submissionSerial(submissionContext.getSubmissionSerial())
                .submissionCreationTime(submissionContext.getSubmissionCreationTime())
                .activityUid(submissionContext.getActivityUid())
                .orgUnitUid(submissionContext.getOrgUnitUid())
                .teamUid(submissionContext.getTeamUid())
                .templateUid(submissionContext.getTemplateUid())
                .startTime(submissionContext.getStartTime())
                .createdAt(now)
                .lastSeen(now)
                .anchorCeId(anchorCeId)
                .anchorValueText(anchorValueText)
                .anchorConfidence(anchorConfidence)
                .anchorResolvedAt(anchorResolvedAt)
                .anchorValueRefType(anchorRefType)
                .anchorRefUid(anchorRefUid)
                .build();

            eventsToPatchUpsert.add(evt);
        }

        // 4) single batch patch-upsert
        try {
            eventJdbcRepository.patchUpsertEvents(eventsToPatchUpsert);
            log.debug("Patched up {} event(s) for submissionUid={}", eventsToPatchUpsert.size(),
                submissionContext.getSubmissionUid());
        } catch (Exception ex) {
            log.error("Failed to patch-upsert events for submissionUid=" + submissionContext.getSubmissionUid(), ex);
            throw ex;
        }

        return rows;
    }
}

