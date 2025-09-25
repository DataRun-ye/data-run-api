//package org.nmcpye.datarun.jpa.extraction.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.jpa.extraction.FieldExtractionDescriptor;
//import org.nmcpye.datarun.jpa.extraction.RawRepeatInstance;
//import org.nmcpye.datarun.jpa.extraction.RepeatExtractionPlan;
//import org.nmcpye.datarun.jpa.extraction.repository.FieldExtractionDescriptorRepository;
//import org.nmcpye.datarun.jpa.extraction.repository.RawRepeatInstanceRepository;
//import org.nmcpye.datarun.jpa.extraction.repository.RepeatExtractionPlanRepository;
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * ExtractionService - projection-agnostic extractor runtime.
// * <p>
// * Consumes:
// * - repeat_extraction_plan (pick latest plan per repeatUid)
// * - field_extraction_descriptor (descriptors for plan)
// * - raw_repeat_instance (unprocessed repeat payloads)
// * <p>
// * Produces:
// * - extracted_neutral_record (one row per RPI occurrence)
// * <p>
// * Important:
// * - This class expects field_extraction_descriptor.json_pointer values to be valid JSON Pointer
// * (leading slash, e.g. "/amd", "/lsm_type"). If not, it will coerce.
// * - It treats values as JsonNodes and stores them as JSONB into extracted_neutral_record.extracted.
// * - Select-multi fields are left as arrays (no explode) by default; descriptor.explode can be used later by projections.
// */
//@Service
//@RequiredArgsConstructor
//public class ExtractionService {
//
//    private final NamedParameterJdbcTemplate jdbc;
//    private final ObjectMapper objectMapper;
//    private final RepeatExtractionPlanRepository planRepo; // JPA repo for repeat_extraction_plan
//    private final FieldExtractionDescriptorRepository descriptorRepo; // JPA repo
//    private final RawRepeatInstanceRepository rpiRepo; // JPA repo for raw_repeat_instance
//
//    /**
//     * Process up to pageSize unprocessed raw_repeat_instance rows for the given repeatUid.
//     * Returns a result with counts and any errors (keeps going on failure per-row).
//     * <p>
//     * Public & transactional so Spring AOP will apply correctly.
//     */
//    @Transactional
//    public ProcessResult processRepeat(String repeatUid, int pageSize) {
//        Objects.requireNonNull(repeatUid, "repeatUid required");
//
//        // 1) Find latest plan for repeatUid
//        Optional<RepeatExtractionPlan> maybePlan = planRepo.findTopByRepeatUidOrderByCreatedAtDesc(repeatUid);
//        if (maybePlan.isEmpty()) {
//            return ProcessResult.failure("no extraction plan for repeatUid=" + repeatUid);
//        }
//        RepeatExtractionPlan plan = maybePlan.get();
//
//        // 2) load descriptors for plan (sorted)
//        List<FieldExtractionDescriptor> descriptors = descriptorRepo.findByPlanIdOrderBySortOrder(plan.getId());
//        Map<String, FieldExtractionDescriptor> descriptorByFieldUid = descriptors.stream()
//            .collect(Collectors.toMap(FieldExtractionDescriptor::getFieldUid, d -> d));
//
//        // 3) fetch unprocessed RPIs for this repeat
//        List<RawRepeatInstance> rpis = rpiRepo.findTopUnprocessedByRepeatUid(repeatUid, pageSize);
//
//        if (rpis.isEmpty()) return ProcessResult.empty();
//
//        int success = 0;
//        List<String> errors = new ArrayList<>();
//
//        for (RawRepeatInstance rpi : rpis) {
//            try {
//                // parse payload (JsonNode)
//                JsonNode payload = rpi.getPayload(); // assuming JPA maps to JsonNode; if String use objectMapper.readTree()
//                // Build extracted JSON object
//                JsonNode extractedJson = buildExtractedJson(payload, descriptors);
//
//                // Build provenance JSON
//                JsonNode provenanceJson = buildProvenance(plan, descriptors);
//
//                // Insert extracted_neutral_record
//                String enrId = UUID.randomUUID().toString();
//                String insertSql = "INSERT INTO extracted_neutral_record (id, plan_id, extraction_snapshot_id, template_version_uid, repeat_uid, raw_payload_id, submission_uid, occurrence_index, extracted, provenance, created_at) " +
//                    "VALUES (:id, :plan_id, :snapshot_id, :tv, :repeat_uid, :raw_payload_id, :submission_uid, :occurrence_index, (:extracted)::jsonb, (:provenance)::jsonb, now())";
//                MapSqlParameterSource params = new MapSqlParameterSource();
//                params.addValue("id", enrId);
//                params.addValue("plan_id", plan.getId());
//                params.addValue("snapshot_id", plan.getSnapshot().getId());
//                params.addValue("tv", rpi.getTemplateVersionUid());
//                params.addValue("repeat_uid", rpi.getRepeatUid());
//                params.addValue("raw_payload_id", rpi.getPayloadId());
//                params.addValue("submission_uid", rpi.getSubmissionUid());
//                params.addValue("occurrence_index", rpi.getOccurrenceIndex());
//                params.addValue("extracted", objectMapper.writeValueAsString(extractedJson));
//                params.addValue("provenance", objectMapper.writeValueAsString(provenanceJson));
//                jdbc.update(insertSql, params);
//
//                // Mark rpi processed (idempotent)
//                String upd = "UPDATE raw_repeat_instance SET processed = true, processed_at = now(), extraction_snapshot_id = :snapshot_id, plan_id = :plan_id WHERE id = :id";
//                MapSqlParameterSource up = new MapSqlParameterSource();
//                up.addValue("snapshot_id", plan.getSnapshot().getId());
//                up.addValue("plan_id", plan.getId());
//                up.addValue("id", rpi.getId());
//                jdbc.update(upd, up);
//
//                success++;
//            } catch (Exception ex) {
//                // Log and continue processing others
//                String msg = String.format("rpi=%s failed: %s", rpi.getId(), ex.getMessage());
//                errors.add(msg);
//            }
//        }
//
//        return ProcessResult.ok(success, errors);
//    }
//
//    // ----- helper: build extracted payload JSON using descriptors -----
//    private JsonNode buildExtractedJson(JsonNode payload, List<FieldExtractionDescriptor> descriptors) {
//        // Create an object node
//        com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
//
//        for (FieldExtractionDescriptor d : descriptors) {
//            String pointer = normalizePointer(d.getJsonPointer());
//            JsonNode valueNode;
//            // Use JsonNode.at() to support JSON Pointer; default to get(fieldName) fallback if at() returns missing
//            valueNode = payload.at(pointer);
//            if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
//                // fallback: remove leading slash and try direct property
//                String fallback = pointer;
//                if (fallback.startsWith("/")) fallback = fallback.substring(1);
//                valueNode = payload.get(fallback);
//            }
//
//            // If still missing, set null; otherwise keep node.
//            if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
//                out.putNull(d.getOutputColumn() != null ? d.getOutputColumn() : d.getFieldUid());
//            } else {
//                // Preserve the JSON structure (use objectMapper to attach)
//                out.set(d.getOutputColumn() != null ? d.getOutputColumn() : d.getFieldUid(), valueNode);
//            }
//        }
//
//        // Also keep entire payload under "extra" for provenance convenience
//        out.set("extra", payload);
//        return out;
//    }
//
//    private String normalizePointer(String p) {
//        if (p == null) return "/";
//        if (p.isEmpty()) return "/";
//        if (!p.startsWith("/")) return "/" + p;
//        return p;
//    }
//
//    private JsonNode buildProvenance(RepeatExtractionPlan plan, List<FieldExtractionDescriptor> descriptors) {
//        com.fasterxml.jackson.databind.node.ObjectNode prov = objectMapper.createObjectNode();
//        prov.put("plan_id", plan.getId());
//        prov.put("snapshot_id", plan.getSnapshot().getId());
//        prov.put("template_version_uid", plan.getTemplateVersionUid());
//        prov.put("created_at", plan.getCreatedAt() == null ? Instant.now().toString() : plan.getCreatedAt().toString());
//        // descriptors summary
//        com.fasterxml.jackson.databind.node.ArrayNode arr = objectMapper.createArrayNode();
//        for (FieldExtractionDescriptor d : descriptors) {
//            com.fasterxml.jackson.databind.node.ObjectNode dd = objectMapper.createObjectNode();
//            dd.put("field_uid", d.getFieldUid());
//            dd.put("element_uid", d.getElementUid());
//            dd.put("json_pointer", d.getJsonPointer());
//            dd.put("value_type", d.getValueType());
//            dd.put("option_set_uid", d.getOptionSetUid());
//            arr.add(dd);
//        }
//        prov.set("descriptors", arr);
//        return prov;
//    }
//
//    // ----- small result DTOs -----
//    public static class ProcessResult {
//        public final boolean ok;
//        public final int processed;
//        public final List<String> errors;
//        public final String message;
//
//        private ProcessResult(boolean ok, int processed, List<String> errors, String message) {
//            this.ok = ok;
//            this.processed = processed;
//            this.errors = errors;
//            this.message = message;
//        }
//
//        public static ProcessResult ok(int processed, List<String> errors) {
//            return new ProcessResult(true, processed, errors, null);
//        }
//
//        public static ProcessResult empty() {
//            return new ProcessResult(true, 0, Collections.emptyList(), "no rows");
//        }
//
//        public static ProcessResult failure(String msg) {
//            return new ProcessResult(false, 0, Collections.singletonList(msg), msg);
//        }
//    }
//}
