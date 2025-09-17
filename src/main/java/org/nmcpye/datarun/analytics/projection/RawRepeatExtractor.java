package org.nmcpye.datarun.analytics.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * RawRepeatExtractor
 * <p>
 * Responsibilities:
 * - Load cached repeat metadata (repeat_uid -> semantic_path) from element_template_config
 * - For a given submission UID, extract all repeat arrays and insert rows into raw_repeat_payload
 * - Be idempotent for a submission+repeat (deletes existing extraction rows before insert)
 * - Offer a backfill helper to iterate submissions for a single repeat
 * <p>
 * Usage:
 * - Autowire this service into your submission handler and call extractForSubmission(submissionUid, formDataJsonNode)
 * or call extractForSubmission(submissionUid) to let it read form_data from the DB.
 * - For initial history, call backfillRepeat(repeatUid, pageSize) from a CommandLineRunner.
 * <p>
 * Notes:
 * - This class uses NamedParameterJdbcTemplate and expects your DB to accept `:payload::jsonb` casting.
 * - It uses gen_random_uuid() in SQL; replace if you prefer uuid_generate_v4().
 *
 * @author Hamza Assada
 * @since 17/09/2025
 */


@Service
public class RawRepeatExtractor {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om;

    // cache repeat metadata to avoid repeated DB hits. Key: repeat_uid -> semantic_path
    private final Map<String, String> repeatCache = new ConcurrentHashMap<>();
    private final ElementTemplateConfigRepository etcRepo;
    private final DataSubmissionRepository submissionRepo;

    // batch size for batch insert
    private final int batchSize = 500;

    public RawRepeatExtractor(NamedParameterJdbcTemplate jdbc, ObjectMapper om,
                              ElementTemplateConfigRepository etcRepo,
                              DataSubmissionRepository submissionRepo) {
        this.jdbc = jdbc;
        this.om = om;
        this.etcRepo = etcRepo;
        this.submissionRepo = submissionRepo;
        loadRepeatCacheFromRepo();
    }

    private void loadRepeatCacheFromRepo() {
        repeatCache.clear();
        List<Object[]> rows = etcRepo.findAllRepeats();
        for (Object[] r : rows) {
            String uid = (String) r[0];
            String path = (String) r[1];
            if (uid != null && path != null) repeatCache.put(uid, path);
        }
    }
    /**
     * Resolve a dot-delimited semantic path inside a JsonNode (returns null if not present)
     */
    private JsonNode resolvePath(JsonNode root, String semanticPath) {
        if (root == null || semanticPath == null || semanticPath.isBlank()) return null;
        String[] parts = semanticPath.split("\\.");
        JsonNode cur = root;
        for (String p : parts) {
            if (cur == null) return null;
            cur = cur.get(p);
        }
        return cur;
    }

    private String extractPayloadId(JsonNode item) {
        JsonNode idNode = item.get("_id");
        if (idNode != null && !idNode.isNull()) return idNode.asText();
        return null;
    }

    /**
     * Extract repeats for a submission by reading form_data from data_submission table.
     * This method will delete any existing rows in raw_repeat_payload for this submission + repeat before inserting.
     */
    @Transactional
    public void extractForSubmission(String submissionUid) throws Exception {
        final var d = submissionRepo.findFormDataJsonByUid(submissionUid);
        final var version = submissionRepo.findTemplateVersionUidByUid(submissionUid);
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("uid", submissionUid);
        String sql = "SELECT form_data, template_uid, template_version_uid FROM data_submission WHERE uid = :uid";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, param);
        if (rows.isEmpty()) {
            // nothing to do
            return;
        }

        // We expect a single row for uid
        Map<String, Object> row = rows.get(0);
        Object formDataObj = row.get("form_data");
        if (formDataObj == null) return;
        JsonNode formData = om.readTree(formDataObj.toString());

        // For each repeat type discovered in cache, attempt extraction
        for (Map.Entry<String, String> e : repeatCache.entrySet()) {
            String repeatUid = e.getKey();
            String semanticPath = e.getValue();
            JsonNode arr = resolvePath(formData, semanticPath);
            if (arr == null || !arr.isArray()) continue;

            // idempotent delete for this submission + repeat
            jdbc.update("DELETE FROM raw_repeat_payload WHERE submission_uid = :s AND repeat_uid = :r",
                new MapSqlParameterSource().addValue("s", submissionUid).addValue("r", repeatUid));

            // prepare batch
            String insertSql = "INSERT INTO raw_repeat_payload (id, submission_uid, repeat_path, occurrence_index, payload, payload_id, repeat_uid, created_at) " +
                "VALUES (:id, :submissionUid, :repeatPath, :occIndex, :payload::jsonb, :payloadId, :repeatUid, now())";

            List<Map<String, Object>> batch = new ArrayList<>();
            int idx = 0;
            for (JsonNode item : arr) {
                Map<String, Object> params = new HashMap<>();
                params.put("id", UUID.randomUUID());
                params.put("submissionUid", submissionUid);
                params.put("repeatPath", semanticPath);
                params.put("occIndex", idx);
                params.put("payload", item.toString());
                params.put("payloadId", extractPayloadId(item));
                params.put("repeatUid", repeatUid);
                batch.add(params);
                idx++;

                if (batch.size() >= batchSize) {
                    jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batch.toArray()));
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batch.toArray()));
            }
        }
    }

    /**
     * Convenience overload: extract for a submission when you already have the formData JsonNode
     */
    @Transactional
    public void extractForSubmission(String submissionUid, JsonNode formData) throws Exception {
        if (formData == null) return;
        for (Map.Entry<String, String> e : repeatCache.entrySet()) {
            String repeatUid = e.getKey();
            String semanticPath = e.getValue();
            JsonNode arr = resolvePath(formData, semanticPath);
            if (arr == null || !arr.isArray()) continue;

            jdbc.update("DELETE FROM raw_repeat_payload WHERE submission_uid = :s AND repeat_uid = :r",
                new MapSqlParameterSource().addValue("s", submissionUid).addValue("r", repeatUid));

            String insertSql = "INSERT INTO raw_repeat_payload (id, submission_uid, repeat_path, occurrence_index, payload, payload_id, repeat_uid, created_at) " +
                "VALUES (:id, :submissionUid, :repeatPath, :occIndex, :payload::jsonb, :payloadId, :repeatUid, now())";

            List<Map<String, Object>> batch = new ArrayList<>();
            int idx = 0;
            for (JsonNode item : arr) {
                Map<String, Object> params = new HashMap<>();
                params.put("id", UUID.randomUUID());
                params.put("submissionUid", submissionUid);
                params.put("repeatPath", semanticPath);
                params.put("occIndex", idx);
                params.put("payload", item.toString());
                params.put("payloadId", extractPayloadId(item));
                params.put("repeatUid", repeatUid);
                batch.add(params);
                idx++;

                if (batch.size() >= batchSize) {
                    jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batch.toArray()));
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batch.toArray()));
        }
    }

    /**
     * Backfill submissions for a given repeatUid. This pages submission UIDs that contain the path and runs extractForSubmission for each.
     * Simple OFFSET/LIMIT paging is used – acceptable for medium-sized datasets. For very large datasets, replace with cursor-based paging.
     */
    public void backfillRepeat(String repeatUid, int pageSize) throws Exception {
        String semanticPath = repeatCache.get(repeatUid);
        if (semanticPath == null) throw new IllegalArgumentException("unknown repeatUid: " + repeatUid);

        String listSql = "SELECT uid FROM data_submission WHERE (form_data #> string_to_array(:path, '.')) IS NOT NULL ORDER BY uid LIMIT :limit OFFSET :offset";
        int offset = 0;
        while (true) {
            MapSqlParameterSource p = new MapSqlParameterSource().addValue("path", semanticPath).addValue("limit", pageSize).addValue("offset", offset);
            List<String> uids = jdbc.query(listSql, p, (rs, rowNum) -> rs.getString("uid"));
            if (uids.isEmpty()) break;
            for (String uid : uids) {
                try {
                    extractForSubmission(uid);
                } catch (Exception ex) { /* log and continue */
                    ex.printStackTrace();
                }
            }
            offset += uids.size();
        }
    }

    /**
     * Force reload of repeat cache (call after you update element_template_config meta)
     */
    public void reloadRepeatCache() {
        repeatCache.clear();
        loadRepeatCache();
    }
}
