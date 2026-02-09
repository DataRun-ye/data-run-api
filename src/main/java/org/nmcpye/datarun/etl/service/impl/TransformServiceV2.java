package org.nmcpye.datarun.etl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @AllArgsConstructor
    public static class ResolutionCandidate {
        public final RefTypeValue refTypeValue;
        public final double confidence;
        public final Instant resolvedAt;
        public final int priority;
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

        Set<String> allInstanceKeys = new HashSet<>();
        Map<UUID, RefTypeValue> refTypeValues = new HashMap<>();
        Map<String, String> instanceRepeatCeMap = new HashMap<>();
        Map<String, String> parentForInstance = new HashMap<>();

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

            String explicitParent = r.getParentInstanceId(); // direct call, assumed present on TallCanonicalRow
            if (explicitParent != null) parentForInstance.put(instanceKey, explicitParent);

            String ceId = r.getCanonicalElementId();
            CanonicalElement ceMeta = templateContext.allCanonicalElementsMap().get(ceId);
            SemanticType semanticType = ceMeta == null ? null : ceMeta.getSemanticType();
            String optionSetUid = ceMeta == null ? null : ceMeta.getOptionSetUid();

            if (semanticType != null && semanticType.isRepeat()) {
                instanceRepeatCeMap.putIfAbsent(instanceKey, ceId);
            }

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

            String eventType = isRoot ? "root" : "repeat";
            String eventCeId = isRoot ? null : instanceRepeatCeMap.get(instanceKey);

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
                .version(submissionContext.getVersion())
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

