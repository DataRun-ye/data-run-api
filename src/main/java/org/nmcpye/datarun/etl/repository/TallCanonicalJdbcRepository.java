package org.nmcpye.datarun.etl.repository;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.etl.model.TallCanonicalRow;
import org.nmcpye.datarun.etl.util.InstanceKeyUtil;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JDBC repository responsible for idempotent upserts into tall_canonical.
 * <p>
 * Idempotency: unique constraint is based on logical source identity via instance_key (COALESCE(repeat_instance_id, submission_uid))
 * and canonical_element_id. ingest_id/outbox_id are provenance only and overwritten on upsert.
 */
@SuppressWarnings("ConcatenationWithEmptyString")
@Repository
@Slf4j
public class TallCanonicalJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;
    final String sql = ""
        + "INSERT INTO analytics.tall_canonical ("
        + "  instance_key, activity_uid, assignment_uid, org_unit_uid, team_uid,  canonical_element_id, outbox_id, ingest_id,"
        + "  submission_id, submission_uid, submission_serial_number, template_uid, template_version_uid,"
        + "  element_path, repeat_instance_id, parent_instance_id, repeat_index,"
        + "  value_text, value_bool, value_number, value_json, value_ref_type, value_ref_uid, submission_creation_time, start_time, created_at, updated_at, is_deleted"
        + ") VALUES ("
        + "  :instance_key, :activity_uid, :assignment_uid, :org_unit_uid, :team_uid, CAST(:canonical_element_id AS uuid), :outbox_id, :ingest_id,"
        + "  :submission_id, :submission_uid, :submission_serial_number, :template_uid, :template_version_uid,"
        + "  :element_path, :repeat_instance_id, :parent_instance_id, :repeat_index,"
        + "  :value_text, :value_bool, :value_number, cast(:value_json AS jsonb), :value_ref_type, :value_ref_uid, :submission_creation_time, :start_time, :created_at, :updated_at, :is_deleted"
        + ") "
        + "ON CONFLICT (instance_key, canonical_element_id) DO UPDATE SET "
        + "  activity_uid = EXCLUDED.activity_uid, "
        + "  assignment_uid = EXCLUDED.assignment_uid, "
        + "  org_unit_uid = EXCLUDED.org_unit_uid, "
        + "  team_uid = EXCLUDED.team_uid, "
        + "  value_text = EXCLUDED.value_text, "
        + "  value_bool = EXCLUDED.value_bool, "
        + "  value_number = EXCLUDED.value_number, "
        + "  value_json = EXCLUDED.value_json, "
        + "  value_ref_type = EXCLUDED.value_ref_type, "
        + "  value_ref_uid = EXCLUDED.value_ref_uid, "
        + "  outbox_id = EXCLUDED.outbox_id, "
        + "  ingest_id = EXCLUDED.ingest_id, "
        + "  submission_creation_time = coalesce(EXCLUDED.submission_creation_time, tall_canonical.submission_creation_time),"
        + "  start_time = coalesce(EXCLUDED.start_time, tall_canonical.start_time),"
        + "  updated_at = now(), "
        + "  is_deleted = EXCLUDED.is_deleted;";

    // truncation thresholds
    private static final int VALUE_TEXT_MAX = 4000;
    private static final int VALUE_JSON_MAX = 10000;

    public TallCanonicalJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Upsert a mixed batch of TallCanonicalRow entries.
     * <p>
     * - Calculates instance_key = COALESCE(repeat_instance_id, submission_uid) and binds it explicitly.
     * - Sanitizes/truncates value_text and value_json to reasonable sizes to avoid extremely large payloads.
     * - Executes a parameterized batchUpdate using MapSqlParameterSource[] for performance.
     * <p>
     * On DB errors this method throws DataAccessException so callers can handle retries/failure.
     */
    public void upsertBatch(List<TallCanonicalRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        MapSqlParameterSource[] params = new MapSqlParameterSource[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            TallCanonicalRow r = rows.get(i);
            String instanceKey = InstanceKeyUtil.computeInstanceKey(r);
            MapSqlParameterSource p = new MapSqlParameterSource();
            p.addValue("instance_key", instanceKey);
            p.addValue("activity_uid", r.getActivity());
            p.addValue("org_unit_uid", r.getOrgUnit());
            p.addValue("team_uid", r.getTeam());
            p.addValue("assignment_uid", r.getAssignment());
            p.addValue("canonical_element_id", r.getCanonicalElementId());
            p.addValue("outbox_id", r.getOutboxId());
            p.addValue("ingest_id", r.getIngestId());
            p.addValue("submission_id", r.getSubmissionId());
            p.addValue("submission_uid", r.getSubmissionUid());
            p.addValue("submission_serial_number", r.getSubmissionSerialNumber());
            p.addValue("submission_creation_time",
                r.getSubmissionCreationTime() != null ? Timestamp.from(r.getSubmissionCreationTime()) : null);
            p.addValue("start_time",
                r.getSubmissionCreationTime() != null ? Timestamp.from(r.getSubmissionStartTime()) : null);
            p.addValue("template_uid", r.getTemplateUid());
            p.addValue("template_version_uid", r.getTemplateVersionUid());
            p.addValue("element_path", r.getElementPath());
            p.addValue("repeat_instance_id", r.getRepeatInstanceId());
            p.addValue("parent_instance_id", r.getParentInstanceId());
            p.addValue("repeat_index", r.getRepeatIndex());
            // sanitize value_text to 4000 chars
            String vt = r.getValueText();
            if (vt != null && vt.length() > VALUE_TEXT_MAX) vt = vt.substring(0, VALUE_TEXT_MAX) + "...(truncated)";
            p.addValue("value_text", vt);
            p.addValue("value_number", r.getValueNumber());
            p.addValue("value_bool", r.getValueBool());
            String vj = r.getValueJson();
            if (vj != null && vj.length() > VALUE_JSON_MAX) vj = vj.substring(0, VALUE_JSON_MAX) + "...(truncated)";
            p.addValue("value_json", vj);
            p.addValue("created_at", r.getCreatedAt() != null ? Timestamp.from(r.getCreatedAt()) : Timestamp.from(Instant.now()));
            p.addValue("updated_at", r.getUpdatedAt() != null ? Timestamp.from(r.getUpdatedAt()) : Timestamp.from(Instant.now()));
            p.addValue("is_deleted", Boolean.TRUE.equals(r.getIsDeleted()));

            p.addValue("value_ref_type", r.getValueRefType());
            p.addValue("value_ref_uid", r.getValueRefUid());

            params[i] = p;
        }

        // execute batch
        try {
            jdbc.batchUpdate(sql, params);
        } catch (Exception ex) {
            // debug: dump first parameter map to inspect what values caused the DB error
            if (params != null && params.length > 0) {
                try {
                    Map<String, Object> first = params[0].getValues();
                    log.error("Tall upsert failed. first-param keys: {}, sample values (trimmed): {}", first.keySet(),
                        first.entrySet().stream()
                            .map(e -> e.getKey() + "=" + (e.getValue() == null ? "null" : String.valueOf(e.getValue()).substring(0, Math.min(200, String.valueOf(e.getValue()).length()))))
                            .limit(10)
                            .collect(Collectors.joining(", ")));
                } catch (Exception e2) {
                    log.error("Failed to dump first params: {}", e2.getMessage());
                }
            }
            log.error("Tall upsert SQL error", ex);
            throw ex;
        }
    }


    /**
     * Mark as deleted any tall_canonical rows for the given submission + canonical_element_id
     * whose instance_key is NOT in the provided keepKeys set.
     * <p>
     * If keepKeys is null or empty, this will mark ALL rows for the submission+canonical_element_id
     * as deleted (useful to model "user cleared all values").
     * <p>
     * This marks rows as deleted (logical delete). If you prefer hard DELETE, replace UPDATE ...
     * SET is_deleted = true, ... with DELETE FROM tall_canonical ... in both SQL strings.
     * <p>
     * It filters AND is_deleted = false to avoid touching already-deleted rows (cheap).
     *
     * @param submissionUid      submissionUid
     * @param canonicalElementId canonicalElementId
     * @param keepKeys           keepKeys
     * @return number of rows updated (marked deleted).
     */
    public int deleteNotIn(String submissionUid, String canonicalElementId, java.util.Collection<String> keepKeys) {
        final String sqlAll = ""
            + "UPDATE analytics.tall_canonical SET is_deleted = true, updated_at = now() "
            + "WHERE submission_uid = :submissionUid "
            + "  AND canonical_element_id = CAST(:canonicalElementId AS uuid) "
            + "  AND is_deleted = false";

        final String sqlNotIn = ""
            + "UPDATE analytics.tall_canonical SET is_deleted = true, updated_at = now() "
            + "WHERE submission_uid = :submissionUid "
            + "  AND canonical_element_id = CAST(:canonicalElementId AS uuid) "
            + "  AND instance_key NOT IN (:keepKeys) "
            + "  AND is_deleted = false";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submissionUid", submissionUid)
            .addValue("canonicalElementId", canonicalElementId);

        if (keepKeys == null || keepKeys.isEmpty()) {
            return jdbc.update(sqlAll, params);
        } else {
            params.addValue("keepKeys", keepKeys);
            return jdbc.update(sqlNotIn, params);
        }
    }

    /**
     * Hard-delete tall rows belonging to a repeat instance (DELETE event).
     * <p>
     * Returns number of rows deleted.
     * <p>
     * Note: deletes are destructive. For BACKFILL/REPLAY prefer idempotent upserts; explicit deletes are used for event_type=DELETE.
     */
    public int deleteByRepeatInstanceId(String repeatInstanceId) {
        final String sql = "DELETE FROM analytics.tall_canonical WHERE repeat_instance_id = :repeatInstanceId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("repeatInstanceId", repeatInstanceId);
        return jdbc.update(sql, params);
    }

    /**
     * Hard-delete tall rows for a submission (used when entire submission is deleted).
     * <p>
     * Returns number of rows deleted.
     */
    public int deleteBySubmissionUid(String submissionUid) {
        final String sql = "DELETE FROM analytics.tall_canonical WHERE submission_uid = :submissionUid";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("submissionUid", submissionUid);
        return jdbc.update(sql, params);
    }
}
