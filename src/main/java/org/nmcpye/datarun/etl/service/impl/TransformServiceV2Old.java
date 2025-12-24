//package org.nmcpye.datarun.etl.service.impl;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.etl.dto.CanonicalElementAnchorDto;
//import org.nmcpye.datarun.etl.dto.EventDto;
//import org.nmcpye.datarun.etl.dto.RefTypeValue;
//import org.nmcpye.datarun.etl.model.SubmissionContext;
//import org.nmcpye.datarun.etl.model.TallCanonicalRow;
//import org.nmcpye.datarun.etl.model.TemplateContext;
//import org.nmcpye.datarun.etl.repository.RefTypeValueRepository;
//import org.nmcpye.datarun.etl.service.EventEntityService;
//import org.nmcpye.datarun.etl.service.SubmissionKeysService;
//import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
//import org.nmcpye.datarun.jpa.datatemplate.SemanticType;
//import org.nmcpye.datarun.utils.UuidUtils;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
///**
// * Lightweight wrapper: reuses TransformServiceRobust to produce TallCanonicalRow list,
// * then resolves ref-values, sets valueRefUid on rows, computes anchors per instance_key,
// * upserts event rows and returns enriched tall rows ready for persistence.
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TransformServiceV2Old {
//
//    private final TransformServiceRobust baseTransform;
//    private final RefResolutionService refResolutionService;
//    private final RefTypeValueResolutionService refTypeValueResolutionService;
//    private final RefTypeValueRepository refTypeValueRepository;
//    private final EventEntityService eventEntityService;
//    private final SubmissionKeysService submissionKeysService;
//
//    public static class ResolutionCandidate {
//        public final RefTypeValue refTypeValue;
//        //        public final String resolvedUid;
////        public final String rawToken;
//        public final double confidence;
//        public final Instant resolvedAt;
//        public final int priority;
//
//        public ResolutionCandidate(RefTypeValue refTypeValue,
//                                   double confidence, Instant resolvedAt, int priority) {
//            this.refTypeValue = refTypeValue;
//            this.confidence = confidence;
//            this.resolvedAt = resolvedAt;
//            this.priority = priority;
//        }
//    }
//
//    /**
//     * Transform + resolve + upsert events.
//     *
//     * @param root                   JSON payload
//     * @param templateContext        template elements and anchors maps
//     * @param flattenPrimitiveArrays flatten flag forwarded to base transform
//     * @param submissionContext      context map with keys: submissionUid, submissionId, templateUid, templateVersionUid, submissionCreationTime, startTime, assignmentUid, activityUid, orgUnitUid, teamUid
//     */
//    public List<TallCanonicalRow> transform(JsonNode root,
//                                            boolean flattenPrimitiveArrays,
//                                            SubmissionContext submissionContext,
//                                            TemplateContext templateContext) {
//
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
//
//        log.debug("TransformV2: submissionUid={}, templateUid={}, templateVersionUid={}, payloadNull={}",
//            submissionContext.getSubmissionUid(), submissionContext.getTemplateUid(),
//            submissionContext.getTemplateVersionUid(), root == null);
//
//        log.debug("TransformV2: templateElements count = {}", templateContext.allCanonicalElementsMap().size());
//
//        // Run existing robust transform to get tall rows (no persistence here)
//        List<TallCanonicalRow> rows = baseTransform.transform(
//            root,
//            // IMPORTANT: baseTransform expects List<TemplateElement> — ensure values are TemplateElement
//            templateContext.allCanonicalElementsMap().values().stream().toList(),
//            flattenPrimitiveArrays
//        );
//
//        if (rows == null || rows.isEmpty()) {
//            log.debug("TransformV2: baseTransform returned no rows for submissionUid={}", submissionUid);
//            return Collections.emptyList();
//        }
//
//        var submissionEvent = EventDto.builder()
//            .eventId(submissionUid)
//            .eventType("root")
//            .eventCeId(null)
//            .submissionUid(submissionUid)
//            .submissionId(submissionId)
//            .submissionSerial(submissionSerial)
//            .submissionCreationTime(submissionCreationTime)
//            .activityUid(activityUid)
//            .orgUnitUid(orgUnitUid)
//            .teamUid(teamUid)
//            .templateUid(templateUid)
//            .startTime(startTime)
////            .createdAt(now)
////            .anchorCeId(anchorCeId)
////            .anchorValueText(anchorValueText)
////            .anchorConfidence(anchorConfidence)
////            .anchorResolvedAt(anchorResolvedAt)
////            .anchorValueRefType(anchorRefType)
////            .anchorRefUid(anchorRefUid);
//            ;
//
//        // <repeat canonical_id -> EventDto
//        Map<String, EventDto> repeatEventsMap = new HashMap<>();
//
//        Map<String, TallCanonicalRow> tcRowsMap = rows.stream()
//            .collect(Collectors.toMap(TallCanonicalRow::getCanonicalElementId, Function.identity()));
//
//        // Iterate rows, resolve refs, collect anchor candidates per instance_key
//        Map<String, List<ResolutionCandidate>> candidatesByInstance = new HashMap<>();
//        // Also collect all observed instance keys so we create events even when no candidate exists
//        Set<String> allInstanceKeys = new HashSet<>();
//
//        // ce -> RefTypeValue
//        Map<UUID, RefTypeValue> refTypeValues = new HashMap<>();
//
//        for (TallCanonicalRow r : rows) {
//            // ensure provenance fields set on row if absent
//            r.setSubmissionUid(submissionUid);
//            r.setSubmissionSerialNumber(submissionSerial);
//            r.setSubmissionId(submissionId);
//            r.setTemplateUid(templateUid);
//            r.setTemplateVersionUid(templateVersionUid);
//            r.setAssignment(assignmentUid);
//            r.setActivity(activityUid);
//            r.setOrgUnit(orgUnitUid);
//            r.setTeam(teamUid);
//
//            String instanceKey = r.getRepeatInstanceId() != null ? r.getRepeatInstanceId() : submissionUid;
//            allInstanceKeys.add(instanceKey);
//
//            String ceId = r.getCanonicalElementId();
//            CanonicalElementAnchorDto anchorDto = templateContext.anchorsMap().get(ceId);
//            CanonicalElement ceMeta =
//                templateContext.allCanonicalElementsMap().get(ceId);
//
//            SemanticType semanticType = ceMeta == null || ceMeta.getSemanticType() == null ? null :
//                ceMeta.getSemanticType();
//            String optionSetUid = ceMeta == null ? null : ceMeta.getOptionSetUid();
//            boolean anchorAllowed = anchorDto != null && Boolean.TRUE.equals(anchorDto.getAnchorAllowed());
//            int anchorPriority = anchorDto == null ? 100 : anchorDto.getAnchorPriority();
//
//            if (semanticType != null) {
//                // only resolve ref semantic types
//                if (semanticType.isRef()) {
//                    String token = r.getValueText();
//                    String refType = semanticType.name();
//                    String resolvedUid = refTypeValueResolutionService.resolve(token, refType, optionSetUid,
//                        submissionContext.getActivityUid());
//                    RefTypeValue refTypeValue = RefTypeValue.builder()
//                        .valueRefUid(resolvedUid)
//                        .ceId(UuidUtils.toUuidOrNull(r.getCanonicalElementId()))
//                        .submissionUid(submissionUid)
//                        .instanceKey(instanceKey)
//                        .templateUid(templateUid)
//                        .rawValue(token)
//                        .refType(refType)
//                        .createdAt(startTime)
//                        .optionSetUid(optionSetUid)
//                        .build();
//                    // choose token from value_text first, else value_json
//                    if (token == null && r.getValueJson() != null) {
//                        refTypeValue.setRawValue(r.getValueJson());
//
//                        // misconfigured MultiSelectOption as Option
//                        r.setValueRefType(refType);
//
//                        refTypeValue.setRefType(refType);
//                    }
//
//                    refTypeValues.putIfAbsent(refTypeValue.getCeId(), refTypeValue);
//
//                    // if anchor allowed, collect candidate (even if resolvedUid==null — we still want identity row)
//                    if (anchorAllowed) {
//                        ResolutionCandidate cand = new ResolutionCandidate(
//                            refTypeValue, refTypeValue.getValueRefUid() == null ? 0 : 1, Instant.now(),
//                            anchorPriority
//                        );
//                        candidatesByInstance.computeIfAbsent(instanceKey, k -> new ArrayList<>()).add(cand);
//                    }
//                } else if (semanticType.isRepeat()) {
//
//                }
//            }
//
//        }
//
//        refTypeValueRepository.batchUpsert(refTypeValues);
//        // For each observed instanceKey, pick best candidate if present, else create event with null anchors
//        for (String instanceKey : allInstanceKeys) {
//            List<ResolutionCandidate> list = candidatesByInstance.getOrDefault(instanceKey, Collections.emptyList());
//
//            ResolutionCandidate best = null;
//            if (!list.isEmpty()) {
//                list.sort(Comparator.comparingInt((ResolutionCandidate c) -> c.priority)
//                    .thenComparingDouble((ResolutionCandidate c) -> -c.confidence)
//                    .thenComparing((ResolutionCandidate c) -> c.resolvedAt == null ? Instant.EPOCH : c.resolvedAt, Comparator.reverseOrder())
//                    .thenComparing(c -> c.refTypeValue.getCeId()));
//                best = list.get(0);
//            }
//
//            String eventType = instanceKey.equals(submissionUid) ? "root" : "repeat";
////            Optional<EventDto> existing = eventEntityService.findByInstanceKey(instanceKey);
////            String eventUid = existing.map(EventDto::eventId).orElse(null);
//
//            Instant now = Instant.now();
//
//            String anchorCeId = best != null ? best.refTypeValue.getCeId().toString() : null;
//            String anchorRefUid = best != null ? best.refTypeValue.getValueRefUid() : null;
//            String anchorRefType = best != null ? best.refTypeValue.getRefType() : null;
//            String anchorValueText = best != null ? best.refTypeValue.getRawValue() : null;
//            BigDecimal anchorConfidence = best != null ? BigDecimal.valueOf(best.confidence) : null;
//            Instant anchorResolvedAt = best != null ? best.resolvedAt : null;
//
//            // Upsert event row; your repo's CASE prevents overwriting a better anchor with nulls.
//            try {
//                eventEntityService.upsert(
//                    EventDto.builder()
//                        .eventId(instanceKey)
//                        .eventType(eventType)
//                        .parentEventId(instanceKey)
//                        .eventCeId()
//                        .submissionUid(submissionUid)
//                        .submissionId(submissionId)
//                        .submissionSerial(submissionSerial)
//                        .submissionCreationTime(submissionCreationTime)
//                        .activityUid(activityUid)
//                        .orgUnitUid(orgUnitUid)
//                        .teamUid(teamUid)
//                        .templateUid(templateUid)
//                        .startTime(startTime)
//                        .createdAt(now)
//                        .anchorCeId(anchorCeId)
//                        .anchorValueText(anchorValueText)
//                        .anchorConfidence(anchorConfidence)
//                        .anchorResolvedAt(anchorResolvedAt)
//                        .anchorValueRefType(anchorRefType)
//                        .anchorRefUid(anchorRefUid)
//                        .build());
//
//                log.debug("Event upserted: instanceKey={}, eventType={}, anchorRefUid={}", instanceKey, eventType, anchorRefUid);
//            } catch (Exception ex) {
//                log.error("Failed to upsert event for instanceKey=" + instanceKey, ex);
//                // do not fail whole transform here; caller will catch if necessary — but log for debugging
//                throw ex;
//            }
//        }
//
//        // Return enriched rows; caller will set final provenance and persist tall rows via tallRepo
//        return rows;
//    }
//}
//
