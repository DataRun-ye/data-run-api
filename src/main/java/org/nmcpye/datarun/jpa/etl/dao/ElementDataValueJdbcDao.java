package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class ElementDataValueJdbcDao implements IElementDataValueDao {

    private final NamedParameterJdbcTemplate jdbc;

    public ElementDataValueJdbcDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // A single, unified UPSERT statement handles all cases.
    private static final String UPSERT_SQL = """
        INSERT INTO element_data_value (
            semantic_path, submission_uid, assignment_uid, team_uid, org_unit_uid, activity_uid,
            canonical_element_uid, manifest_uid, etc_uid, repeat_instance_id, option_uid,
            value_text, value_num, value_bool, value_ts, value_ref_uid, row_type, created_date, last_modified_date, deleted_at
        ) VALUES (
            :semanticPath, :submissionUid, :assignmentUid, :teamUid, :orgUnitUid, :activityUid,
            :canonicalElementUid, :manifestUid, :etcUid, :repeatInstanceId, :optionUid,
            :valueText, :valueNum, :valueBool, :valueTs, :valueRefUid,:rowType, :createdDate, :lastModifiedDate, NULL
        )
        ON CONFLICT (submission_uid, canonical_path, repeat_instance_key, row_type, selection_key)
        DO UPDATE SET
            assignment_uid = EXCLUDED.assignment_uid,
            team_uid = EXCLUDED.team_uid,
            org_unit_uid = EXCLUDED.org_unit_uid,
            activity_uid = EXCLUDED.activity_uid,
            value_text = EXCLUDED.value_text,
            value_num = EXCLUDED.value_num,
            value_bool = EXCLUDED.value_bool,
            value_ts = EXCLUDED.value_ts,
            value_ref_uid = EXCLUDED.value_ref_uid,
            etc_uid = EXCLUDED.etc_uid,
            canonical_element_uid = EXCLUDED.canonical_element_uid,
           -- manifest_uid = EXCLUDED.manifest_uid,
            last_modified_date = now(),
            deleted_at = NULL;
        """;

    @Override
    public void upsertSubmissionValuesBatch(List<ElementDataValue> rows) {
        if (rows == null || rows.isEmpty()) return;

        SqlParameterSource[] batch = rows.stream()
            .map(this::toParamSource)
            .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(UPSERT_SQL, batch);
    }

    @Override
    public void markAllAsDeletedForSubmission(String submissionUid) {
        String sql = "UPDATE element_data_value SET deleted_at = now(), last_modified_date = now() " +
            "WHERE submission_uid = :submissionUid AND deleted_at IS NULL";
        jdbc.update(sql, new MapSqlParameterSource("submissionUid", submissionUid));
    }

    private MapSqlParameterSource toParamSource(ElementDataValue r) {
        // Ensure timestamps are set for new rows
        if (r.getCreatedDate() == null) r.setCreatedDate(Instant.now());
        if (r.getLastModifiedDate() == null) r.setLastModifiedDate(Instant.now());

        return new MapSqlParameterSource()
            .addValue("semanticPath", r.getCanonicalPath())
            .addValue("submissionUid", r.getSubmissionUid())
            .addValue("assignmentUid", r.getAssignmentUid())
            .addValue("teamUid", r.getTeamUid())
            .addValue("orgUnitUid", r.getOrgUnitUid())
            .addValue("activityUid", r.getActivityUid())
            .addValue("canonicalElementUid", r.getCanonicalElementUid())
            .addValue("etcUid", r.getEtcUid())
            .addValue("manifestUid", r.getManifestUid())
            .addValue("valueType", r.getValueType())
            .addValue("repeatInstanceId", r.getRepeatInstanceId())
            .addValue("optionUid", r.getOptionUid())
            .addValue("valueText", r.getValueText())
            .addValue("valueNum", r.getValueNum())
            .addValue("valueBool", r.getValueBool())
            .addValue("valueRefUid", r.getValueRefUid())
            .addValue("valueTs", r.getValueTs() != null ? Timestamp.from(r.getValueTs()) : null)
            .addValue("rowType", r.getOptionUid() != null ? "M" : "S") // Set row_type dynamically
            .addValue("createdDate", Timestamp.from(r.getCreatedDate()))
            .addValue("lastModifiedDate", Timestamp.from(r.getLastModifiedDate()));
    }
}
