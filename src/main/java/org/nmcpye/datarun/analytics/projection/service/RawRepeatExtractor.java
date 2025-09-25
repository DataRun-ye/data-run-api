//package org.nmcpye.datarun.analytics.projection.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
//import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateElementRepository;
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.jdbc.core.namedparam.SqlParameterSource;
//import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.sql.Types;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * RawRepeatExtractor
// * <p>
// * Responsibilities:
// * - Load cached repeat metadata (repeat_uid -> semantic_path) from template_element
// * - For a given submission UID, extract all repeat arrays and insert rows into raw_repeat_payload
// * - Be idempotent for a submission+repeat (deletes existing extraction rows before insert)
// * - Offer a backfill helper to iterate submissions for a single repeat
// * <p>
// * Usage:
// * - Autowire this service into your submission handler and call extractForSubmission(submissionUid, formDataJsonNode)
// * or call extractForSubmission(submissionUid) to let it read form_data from the DB.
// * - For initial history, call backfillRepeat(repeatUid, pageSize) from a CommandLineRunner.
// * <p>
// * Notes:
// * - This class uses NamedParameterJdbcTemplate and expects your DB to accept `:payload::jsonb` casting.
// * - It uses gen_random_uuid() in SQL; replace if you prefer uuid_generate_v4().
// *
// * @author Hamza Assada
// * @since 17/09/2025
// */
//@Service
//public class RawRepeatExtractor {
//    private final NamedParameterJdbcTemplate jdbc;
//    private final ObjectMapper om;
//
//    // cache repeat metadata to avoid repeated DB hits. Key: repeat_uid -> semantic_path
//    private final Map<String, String> repeatCache = new ConcurrentHashMap<>();
//    private final TemplateElementRepository etcRepo;
//    private final DataSubmissionRepository submissionRepo;
//
//    // batch size for batch insert
//    private final int batchSize = 500;
//
//    public RawRepeatExtractor(NamedParameterJdbcTemplate jdbc, ObjectMapper om,
//                              TemplateElementRepository etcRepo,
//                              DataSubmissionRepository submissionRepo) {
//        this.jdbc = jdbc;
//        this.om = om;
//        this.etcRepo = etcRepo;
//        this.submissionRepo = submissionRepo;
//        loadRepeatCacheFromRepo();
//    }
//
//    /**
//     * Extract repeats for a submission by reading form_data from data_submission table.
//     * This method will delete any existing rows in raw_repeat_payload for this submission + repeat before inserting.
//     */
//    @Transactional
//    public void extractForSubmission(String submissionUid) throws Exception {
//        final var templateVersionUidFromSubmission = submissionRepo.findTemplateVersionUidByUid(submissionUid);
//        MapSqlParameterSource param = new MapSqlParameterSource().addValue("uid", submissionUid);
//        String sql = "SELECT form_data, template_uid, template_version_uid FROM data_submission WHERE uid = :uid";
//        List<Map<String, Object>> rows = jdbc.queryForList(sql, param);
//        if (rows.isEmpty()) {
//            // nothing to do
//            return;
//        }
//
//        // We expect a single row for uid
//        Map<String, Object> row = rows.get(0);
//        Object formDataObj = row.get("form_data");
//        if (formDataObj == null) return;
//        JsonNode formData = om.readTree(formDataObj.toString());
//
//        // For each repeat type discovered in cache, attempt extraction
//        for (Map.Entry<String, String> e : repeatCache.entrySet()) {
//            String repeatUid = e.getKey();
//            String semanticPath = e.getValue();
//            JsonNode arr = resolvePath(formData, semanticPath);
//            if (arr == null || !arr.isArray()) continue;
//
//            // idempotent delete for this submission + repeat
//            jdbc.update("DELETE FROM raw_repeat_payload WHERE submission_uid = :s AND repeat_uid = :r",
//                new MapSqlParameterSource().addValue("s", submissionUid).addValue("r", repeatUid));
//
//            // prepare batch
//            String insertSql = "INSERT INTO raw_repeat_payload (\n" +
//                "  id, submission_uid, repeat_path, occurrence_index, " +
//                "payload, payload_id, payload_parent_id, repeat_uid, template_version_uid, created_at) " +
//                "VALUES(:id, :submissionUid, :repeatPath, :occIndex, " +
//                ":payload::jsonb, :payloadId, :payloadParentId, :repeatUid, :templateVersionUid, now())";
//
//            List<Map<String, Object>> batch = new ArrayList<>();
//            int idx = 0;
//            for (JsonNode item : arr) {
//                Map<String, Object> params = new HashMap<>();
//                params.put("id", CodeGenerator.nextUlid());
//                params.put("submissionUid", submissionUid);
//                params.put("repeatPath", semanticPath);
//                params.put("occIndex", idx);
//                params.put("payload", item.toString());
//                params.put("payloadId", extractPayloadId(item));
//                params.put("repeatUid", repeatUid);
//                params.put("payloadParentId", item.has("_parentId") ? item.get("_parentId").asText() : null);
//                params.put("templateVersionUid", templateVersionUidFromSubmission);
//                batch.add(params);
//                idx++;
//
//                if (batch.size() >= batchSize) {
//                    jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batch.toArray()));
//                    batch.clear();
//                }
//            }
//            if (!batch.isEmpty()) {
//                jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batch.toArray()));
//            }
//        }
//    }
//
//    /**
//     * Convenience overload: extract for a submission when you already have the formData JsonNode
//     */
//    @Transactional
//    public void extractForSubmission(String submissionUid, JsonNode formData) throws Exception {
//        final var templateVersionUidFromSubmission = submissionRepo.findTemplateVersionUidByUid(submissionUid);
//
//        if (formData == null) return;
//        for (Map.Entry<String, String> e : repeatCache.entrySet()) {
//            String repeatUid = e.getKey();
//            String semanticPath = e.getValue();
//            JsonNode arr = resolvePath(formData, semanticPath);
//            if (arr == null || !arr.isArray()) continue;
//
//            jdbc.update("DELETE FROM raw_repeat_payload WHERE submission_uid = :s AND repeat_uid = :r",
//                new MapSqlParameterSource().addValue("s", submissionUid).addValue("r", repeatUid));
//
//            // prepare batch
//            String insertSql = "INSERT INTO raw_repeat_payload (\n" +
//                "  id, submission_uid, repeat_path, occurrence_index, " +
//                "payload, payload_id, payload_parent_id, repeat_uid, template_version_uid, created_at) " +
//                "VALUES(:id, :submissionUid, :repeatPath, :occIndex, " +
//                ":payload::jsonb, :payloadId, :payloadParentId, :repeatUid, :templateVersionUid, now())";
//
//            List<MapSqlParameterSource> batch = new ArrayList<>();
//
//            int idx = 0;
//            for (JsonNode item : arr) {
//                batch.add(new MapSqlParameterSource().addValue("id", CodeGenerator.nextUlid())
//                    .addValue("submissionUid", submissionUid)
//                    .addValue("repeatPath", semanticPath)
//                    .addValue("occIndex", idx)
//                    .addValue("payload", toJsonbObject(item.toString()), Types.OTHER)
//                    .addValue("payloadId", extractPayloadId(item))
//                    .addValue("repeatUid", repeatUid)
//                    .addValue("payloadParentId", item.has("_parentId") ? item.get("_parentId").asText() : null)
//                    .addValue("templateVersionUid", templateVersionUidFromSubmission));
//                idx++;
//
//                if (batch.size() >= batchSize) {
//                    jdbc.batchUpdate(insertSql, batch.toArray(SqlParameterSource[]::new));
//                    batch.clear();
//                }
//            }
//            if (!batch.isEmpty()) jdbc.batchUpdate(insertSql, batch.toArray(SqlParameterSource[]::new));
//        }
//    }
//
//    /**
//     * Force reload of repeat cache (call after you update template_element meta)
//     */
//    public void reloadRepeatCache() {
//        repeatCache.clear();
//        loadRepeatCacheFromRepo();
//    }
//
//
//    private void loadRepeatCacheFromRepo() {
//        repeatCache.clear();
//        List<Object[]> rows = etcRepo.findAllRepeats();
//        for (Object[] r : rows) {
//            String uid = (String) r[0];
//            String path = (String) r[1];
//            if (uid != null && path != null) repeatCache.put(uid, path);
//        }
//    }
//
//    /**
//     * Resolve a dot-delimited semantic path inside a JsonNode (returns null if not present)
//     */
//    private JsonNode resolvePath(JsonNode root, String semanticPath) {
//        if (root == null || semanticPath == null || semanticPath.isBlank()) return null;
//        String[] parts = semanticPath.split("\\.");
//        JsonNode cur = root;
//        for (String p : parts) {
//            if (cur == null) return null;
//            cur = cur.get(p);
//        }
//        return cur;
//    }
//
//    private String extractPayloadId(JsonNode item) {
//        JsonNode idNode = item.get("_id");
//        if (idNode != null && !idNode.isNull()) return idNode.asText();
//        return null;
//    }
//
//    private Object toJsonbObject(String json) {
//        if (json == null) return null;
//        try {
//            org.postgresql.util.PGobject pg = new org.postgresql.util.PGobject();
//            pg.setType("jsonb");
//            pg.setValue(json);
//            return pg;
//        } catch (Exception e) {
//            throw new IllegalStateException("Failed to convert label to jsonb", e);
//        }
//    }
//}
