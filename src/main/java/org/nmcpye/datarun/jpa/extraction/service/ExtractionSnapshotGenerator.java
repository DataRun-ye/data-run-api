//package org.nmcpye.datarun.jpa.extraction.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
//import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.nmcpye.datarun.jpa.option.repository.OptionSetRepository;
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.time.OffsetDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * ExtractionSnapshotGenerator
// * <p>
// * Reads Layer-0 ElementTemplateConfig rows for a templateVersion and generates:
// * - template_extraction_snapshot (immutable manifest JSON + checksum)
// * - repeat_extraction_plan (one per repeat)
// * - field_extraction_descriptor rows for each field under repeat
// * <p>
// * Uses JPA repos for reads and NamedParameterJdbcTemplate for inserts into extraction layer tables.
// * <p>
// * Assumptions:
// * - ElementTemplateConfigRepository exists and returns ETC rows for a templateVersionUid.
// * - ETC rows contain: element_kind, data_element_uid, semantic_path, repeat_uid, option_set_uid,
// * value_type, is_multi, is_natural_key_candidate, natural_key_candidates, template_version_uid,
// * id_path, name_path, definition_json, analytics_intent, etc.
// * <p>
// * Adjust repo names and column names as needed.
// */
//@Service
//public class ExtractionSnapshotGenerator {
//
//    private final ElementTemplateConfigRepository etcRepo;
//    private final DataElementRepository dataElementRepo;
//    private final OptionSetRepository optionSetRepo; // optional
//    private final NamedParameterJdbcTemplate jdbc;
//    private final ObjectMapper objectMapper;
//
//    private static final String GENERATOR_VERSION = "v1";
//
//    public ExtractionSnapshotGenerator(
//        ElementTemplateConfigRepository etcRepo,
//        DataElementRepository dataElementRepo,
//        OptionSetRepository optionSetRepo,
//        NamedParameterJdbcTemplate jdbc,
//        ObjectMapper objectMapper
//    ) {
//        this.etcRepo = etcRepo;
//        this.dataElementRepo = dataElementRepo;
//        this.optionSetRepo = optionSetRepo;
//        this.jdbc = jdbc;
//        this.objectMapper = objectMapper;
//    }
//
//    /**
//     * Public entry. Creates a snapshot (idempotent).
//     */
//    @Transactional
//    public SnapshotResult generateSnapshot(String templateVersionUid) throws Exception {
//        // 1. Load ETC rows for this template version
//        List<ElementTemplateConfig> etcRows = etcRepo.findByTemplateVersionUid(templateVersionUid);
//        if (etcRows == null || etcRows.isEmpty()) {
//            throw new IllegalArgumentException("No ElementTemplateConfig rows found for templateVersionUid=" + templateVersionUid);
//        }
//
//        // 2. Build manifest JSON (group by repeat)
//        ObjectNode manifest = objectMapper.createObjectNode();
//        manifest.put("templateVersionUid", templateVersionUid);
//        manifest.put("generatorVersion", GENERATOR_VERSION);
//        manifest.set("generatedAt", objectMapper.valueToTree(OffsetDateTime.now().toString()));
//
//        // map repeatUid -> repeat definition and its child fields
//        Map<String, RepeatBucket> repeats = new LinkedHashMap<>();
//
//        // first collect repeats
//        etcRows.forEach(etc -> {
//            if (etc.isRepeat()) {
//                String rptUid = etc.getRepeatUid() != null ? etc.getRepeatUid() : etc.getDataElementUid();
//                RepeatBucket rb = repeats.computeIfAbsent(rptUid, k -> new RepeatBucket());
//                rb.repeatUid = rptUid;
//                rb.semanticRepeatPath = etc.getSemanticPath(); // e.g. "supply" or "mainsection.breedingsources"
//                rb.extractionPath = deriveExtractionPath(etc); // derive '/supply' etc
//                rb.templateVersionUid = templateVersionUid;
//                rb.repeatDefinition = etc.getDefinitionJson();
//            }
//        });
//
//        // then attach fields to their repeat
//        etcRows.forEach(etc -> {
//            if (!etc.isRepeat()) {
//                String parentRepeatUid = etc.getRepeatUid();
//                if (parentRepeatUid == null) {
//                    // element has no repeat ancestor -> treat as root-level repeat (pseudo) or ignore
//                    parentRepeatUid = "__ROOT__"; // optional handling - skip or include
//                }
//                RepeatBucket rb = repeats.get(parentRepeatUid);
//                if (rb == null) {
//                    // rare: fields whose parent repeat is not present as REPEAT element for this template version
//                    // create a synthetic bucket keyed by parentRepeatUid
//                    rb = repeats.computeIfAbsent(parentRepeatUid, k -> new RepeatBucket());
//                    rb.repeatUid = parentRepeatUid;
//                    rb.semanticRepeatPath = parentRepeatUid;
//                    rb.extractionPath = "/" + parentRepeatUid;
//                    rb.templateVersionUid = templateVersionUid;
//                }
//                FieldDescriptor fd = toFieldDescriptor(etc);
//                rb.fieldDescriptors.add(fd);
//            }
//        });
//
//        // convert repeats map to manifest JSON
//        List<ObjectNode> repeatList = new ArrayList<>();
//        for (RepeatBucket rb : repeats.values()) {
//            ObjectNode rbNode = objectMapper.createObjectNode();
//            rbNode.put("repeatUid", rb.repeatUid);
//            rbNode.put("semanticRepeatPath", rb.semanticRepeatPath == null ? "" : rb.semanticRepeatPath);
//            rbNode.put("extractionPath", rb.extractionPath == null ? "" : rb.extractionPath);
//            rbNode.set("repeatDefinition", rb.repeatDefinition == null ? objectMapper.createObjectNode() : rb.repeatDefinition);
//            // fields
//            List<ObjectNode> fields = rb.fieldDescriptors.stream().map(fd -> {
//                ObjectNode f = objectMapper.createObjectNode();
//                f.put("elementUid", fd.elementUid);
//                f.put("fieldUid", fd.fieldUid);
//                f.put("jsonPointer", fd.jsonPointer);
//                f.put("dataType", fd.dataType);
//                f.put("optionSetUid", fd.optionSetUid);
//                f.put("isMulti", fd.isMulti);
//                f.put("explode", fd.explode);
//                f.put("isNaturalKeyCandidate", fd.isNaturalKeyCandidate);
//                f.put("isMeasure", fd.isMeasure);
//                f.put("isDimension", fd.isDimension);
//                if (fd.postTransform != null) f.set("postTransform", fd.postTransform);
//                if (fd.postTransformRaw != null) {
//                    try {
//                        f.put("postTransformRaw", objectMapper.writeValueAsString(fd.postTransformRaw));
//                    } catch (Exception ignored) {
//                    }
//                }
//                return f;
//            }).collect(Collectors.toList());
//            rbNode.set("fields", objectMapper.valueToTree(fields));
//            repeatList.add(rbNode);
//        }
//        manifest.set("repeats", objectMapper.valueToTree(repeatList));
//
//        // 3. Compute checksum
//        String manifestJson = objectMapper.writeValueAsString(manifest);
//        String checksum = sha256(manifestJson);
//
//        // 4. If a snapshot exists for this template_version with same checksum -> return it
//        String existingSnapshotId = findSnapshotIdByTemplateVersionAndChecksum(templateVersionUid, checksum);
//        if (existingSnapshotId != null) {
//            return SnapshotResult.existing(existingSnapshotId, checksum, manifest);
//        }
//
//        // 5. Persist snapshot + repeat plans + descriptors (transactional)
//        String snapshotId = UUID.randomUUID().toString();
//        insertTemplateExtractionSnapshot(snapshotId, templateVersionUid, GENERATOR_VERSION, checksum, manifestJson);
//
//        // persist repeat plans & descriptors
//        for (RepeatBucket rb : repeats.values()) {
//            String planId = UUID.randomUUID().toString();
//            int descriptorCount = rb.fieldDescriptors.size();
//            insertRepeatExtractionPlan(planId, snapshotId, templateVersionUid, rb.repeatUid,
//                rb.semanticRepeatPath, rb.extractionPath, "PAYLOAD_ID", null, null, descriptorCount);
//            // batch insert field descriptors for this plan
//            List<MapSqlParameterSource> batch = new ArrayList<>();
//            int order = 0;
//            for (FieldDescriptor fd : rb.fieldDescriptors) {
//                MapSqlParameterSource p = new MapSqlParameterSource();
//                String fdId = UUID.randomUUID().toString();
//                p.addValue("id", fdId);
//                p.addValue("plan_id", planId);
//                p.addValue("element_uid", fd.elementUid);
//                p.addValue("field_uid", fd.fieldUid);
//                p.addValue("json_pointer", fd.jsonPointer);
//                p.addValue("value_type", fd.dataType);
//                p.addValue("option_set_uid", fd.optionSetUid);
//                p.addValue("is_multi", fd.isMulti);
//                p.addValue("explode", fd.explode);
//                p.addValue("is_natural_key_candidate", fd.isNaturalKeyCandidate);
//                p.addValue("is_measure", fd.isMeasure);
//                p.addValue("is_dimension", fd.isDimension);
//                p.addValue("output_column", fd.outputColumn);
//                try {
//                    p.addValue("post_transform", fd.postTransform == null ? null : fd.postTransform.toString());
//                } catch (Exception ex) {
//                    p.addValue("post_transform", null);
//                }
//                p.addValue("sort_order", order++);
//                batch.add(p);
//            }
//            if (!batch.isEmpty()) {
//                batchInsertFieldDescriptors(batch);
//            }
//        }
//
//        return SnapshotResult.created(snapshotId, checksum, manifest);
//    }
//
//    /**
//     * Public helper to fetch snapshot id by template version + checksum
//     */
//    public String findSnapshotIdByTemplateVersionAndChecksum(String templateVersionUid, String checksum) {
//        String sql = "SELECT id FROM template_extraction_snapshot WHERE template_version_uid = :tv AND checksum = :checksum LIMIT 1";
//        MapSqlParameterSource p = new MapSqlParameterSource();
//        p.addValue("tv", templateVersionUid);
//        p.addValue("checksum", checksum);
//        List<String> ids = jdbc.query(sql, p, (rs, rowNum) -> rs.getString("id"));
//        return ids.isEmpty() ? null : ids.get(0);
//    }
//
//    /**
//     * Insert template_extraction_snapshot row
//     */
//    private void insertTemplateExtractionSnapshot(String id, String templateVersionUid, String generatorVersion, String checksum, String manifestJson) {
//        String sql = "INSERT INTO template_extraction_snapshot (id, template_version_uid, generator_version, checksum, payload, created_at) " +
//            "VALUES (:id, :tv, :gv, :checksum, (:payload)::jsonb, now())";
//        MapSqlParameterSource p = new MapSqlParameterSource();
//        p.addValue("id", id);
//        p.addValue("tv", templateVersionUid);
//        p.addValue("gv", generatorVersion);
//        p.addValue("checksum", checksum);
//        p.addValue("payload", manifestJson);
//        jdbc.update(sql, p);
//    }
//
//    private void insertCanonicalElement(String id, String templateVersionUid, String generatorVersion, String checksum, String manifestJson) {
//        String sql = "INSERT INTO canonical_element (canonical_element_uid, preferred_name, data_type, semantic_type, " +
//            "canonical_path, cardinality, option_set_id,canonical_candidates, notes, created_date)" +
//            "            SELECT DISTINCT gen_random_uuid(), schema_fingerprint, name_path AS preferred_name, now()\n" +
//            "            FROM template_element te\n" +
//            "            WHERE te.schema_fingerprint IS NOT NULL\n" +
//            "              AND NOT EXISTS (\n" +
//            "                SELECT 1 FROM canonical_element ce WHERE ce.schema_fingerprint = te.schema_fingerprint\n" +
//            "            );";
////        String sql = "INSERT INTO template_extraction_snapshot (id, template_version_uid, generator_version, checksum, payload, created_at) " +
////            "VALUES (:id, :tv, :gv, :checksum, (:payload)::jsonb, now())";
//        MapSqlParameterSource p = new MapSqlParameterSource();
//        p.addValue("id", id);
//        p.addValue("tv", templateVersionUid);
//        p.addValue("gv", generatorVersion);
//        p.addValue("checksum", checksum);
//        p.addValue("payload", manifestJson);
//        jdbc.update(sql, p);
//    }
//
//    /**
//     * Insert repeat_extraction_plan
//     */
//    private void insertRepeatExtractionPlan(String id, String snapshotId, String templateVersionUid, String repeatUid,
//                                            String semanticRepeatPath, String extractionPath, String naturalKeyStrategy,
//                                            String payloadHintJson, String flagsJson, int descriptorCount) {
//        String sql = "INSERT INTO repeat_extraction_plan (id, snapshot_id, template_version_uid, repeat_uid, semantic_repeat_path, extraction_path, natural_key_strategy, payload_hint, flags, descriptor_count, created_at) " +
//            "VALUES (:id, :snapshot_id, :tv, :repeat_uid, :semantic, :extraction, :nk, (:payload_hint)::jsonb, (:flags)::jsonb, :dc, now())";
//        MapSqlParameterSource p = new MapSqlParameterSource();
//        p.addValue("id", id);
//        p.addValue("snapshot_id", snapshotId);
//        p.addValue("tv", templateVersionUid);
//        p.addValue("repeat_uid", repeatUid);
//        p.addValue("semantic", semanticRepeatPath);
//        p.addValue("extraction", extractionPath);
//        p.addValue("nk", naturalKeyStrategy);
//        p.addValue("payload_hint", payloadHintJson == null ? "{}" : payloadHintJson);
//        p.addValue("flags", flagsJson == null ? "{}" : flagsJson);
//        p.addValue("dc", descriptorCount);
//        jdbc.update(sql, p);
//    }
//
//    /**
//     * Batch insert field descriptors for a plan
//     */
//    private void batchInsertFieldDescriptors(List<MapSqlParameterSource> batch) {
//        String sql = "INSERT INTO field_extraction_descriptor (id, plan_id, element_uid, field_uid, json_pointer, value_type, option_set_uid, is_multi, explode, is_natural_key_candidate, is_measure, is_dimension, output_column, post_transform, sort_order, created_at) " +
//            "VALUES (:id, :plan_id, :element_uid, :field_uid, :json_pointer, :value_type, :option_set_uid, :is_multi, :explode, :is_natural_key_candidate, :is_measure, :is_dimension, :output_column, (:post_transform)::jsonb, :sort_order, now())";
//        MapSqlParameterSource[] arr = batch.toArray(new MapSqlParameterSource[0]);
//        jdbc.batchUpdate(sql, arr);
//    }
//
//    // -- helpers --
//
//    private FieldDescriptor toFieldDescriptor(ElementTemplateConfig etc) {
//        FieldDescriptor fd = new FieldDescriptor();
//        fd.elementUid = safe(etc.getDataElementUid());
//        fd.fieldUid = safe(etc.getUid()); // your ETC unique id column getter
//        // Prefer name_path (UI-level name) for json pointer; fallback to semantic_path or id_path
//        String namePath = etc.getNamePath();
//        String semanticPath = etc.getSemanticPath();
//        String repeatPrefix = etc.getRepeatUid();
//        // derive json pointer relative to repeat
//        String jsonPointer = "/";
//        if (namePath != null && repeatPrefix != null && namePath.startsWith(repeatPrefix + ".")) {
//            jsonPointer += namePath.substring(repeatPrefix.length() + 1); // remove "supply." => "amd"
//        } else if (namePath != null && namePath.contains(".")) {
//            // take last segment
//            jsonPointer += namePath.substring(namePath.lastIndexOf('.') + 1);
//        } else if (semanticPath != null && semanticPath.contains(".")) {
//            jsonPointer += semanticPath.substring(semanticPath.lastIndexOf('.') + 1);
//        } else {
//            jsonPointer += (etc.getName() != null ? etc.getName() : etc.getUid());
//        }
//        fd.jsonPointer = jsonPointer;
//        // value type
//        fd.dataType = safe(etc.getValueType().name());
//        fd.optionSetUid = safe(etc.getOptionSetUid());
//        fd.isMulti = Boolean.TRUE.equals(etc.getIsMulti());
//        fd.explode = false; // default; can be set true via analytics_intent or definition_json hints
//        fd.isNaturalKeyCandidate = Boolean.TRUE.equals(etc.getIsNaturalKeyCandidate());
//        // derive measure/dimension hints (ETC may have them or data element)
//        fd.isMeasure = Boolean.TRUE.equals(etc.getIsMeasure());
//        fd.isDimension = Boolean.TRUE.equals(etc.getIsDimension());
//        fd.outputColumn = safe(etc.getName()); // suggestion only
//        // post transform hints: e.g. date format inside definition_json
//        JsonNode def = etc.getDefinitionJson();
//        if (def != null && def.has("type") && "Date".equalsIgnoreCase(def.get("type").asText())) {
//            ObjectNode pt = objectMapper.createObjectNode();
//            pt.put("cast", "date");
//            // optionally detect format if present
//            fd.postTransform = pt;
//            fd.postTransformRaw = def;
//        }
//        return fd;
//    }
//
//    private String deriveExtractionPath(ElementTemplateConfig repeatEtc) {
//        // prefer definition_json extraction path or semantic_path
//        String semantic = safe(repeatEtc.getSemanticPath());
//        if (semantic == null || semantic.isEmpty()) return "/";
//        // ensure it's a json-pointer style path e.g. '/supply' or '/mainsection/breedingsources'
//        if (!semantic.contains(".")) {
//            return "/" + semantic;
//        } else {
//            return "/" + semantic.replace('.', '/');
//        }
//    }
//
//    private static <T> T safe(T s) {
//        return s == null ? null : s;
//    }
//
//    private static JsonNode safeReadJson(ObjectMapper om, String rawJson) {
//        if (rawJson == null) return null;
//        try {
//            return om.readTree(rawJson);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    private static String sha256(String s) throws Exception {
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
//        StringBuilder sb = new StringBuilder();
//        for (byte b : digest) sb.append(String.format("%02x", b));
//        return sb.toString();
//    }
//
//    // ----- DTO / helper classes -----
//
//    private static class RepeatBucket {
//        String repeatUid;
//        String semanticRepeatPath;
//        String extractionPath;
//        String templateVersionUid;
//        JsonNode repeatDefinition;
//        List<FieldDescriptor> fieldDescriptors = new ArrayList<>();
//    }
//
//    private static class FieldDescriptor {
//        public String elementUid;
//        public String fieldUid;
//        public String jsonPointer;
//        public String dataType;
//        public String optionSetUid;
//        public boolean isMulti;
//        public boolean explode;
//        public boolean isNaturalKeyCandidate;
//        public boolean isMeasure;
//        public boolean isDimension;
//        public String outputColumn;
//        public ObjectNode postTransform;
//        public JsonNode postTransformRaw;
//    }
//
//    public static class SnapshotResult {
//        public final boolean created;
//        public final String snapshotId;
//        public final String checksum;
//        public final JsonNode manifest;
//
//        private SnapshotResult(boolean created, String snapshotId, String checksum, JsonNode manifest) {
//            this.created = created;
//            this.snapshotId = snapshotId;
//            this.checksum = checksum;
//            this.manifest = manifest;
//        }
//
//        public static SnapshotResult existing(String snapshotId, String checksum, JsonNode manifest) {
//            return new SnapshotResult(false, snapshotId, checksum, manifest);
//        }
//
//        public static SnapshotResult created(String snapshotId, String checksum, JsonNode manifest) {
//            return new SnapshotResult(true, snapshotId, checksum, manifest);
//        }
//    }
//}
