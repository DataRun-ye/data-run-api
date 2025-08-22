package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

/**
 * Named-parameter JDBC DAO for element_data_value.
 * <p>
 * Important: this class expects the following partial unique indexes exist:
 * - ux_element_single_value  (option_id IS NULL)
 * - ux_element_multi_value   (option_id IS NOT NULL)
 * <p>
 * and the table element_data_value with columns including option_id, value_text, deleted_at etc.
 *
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Repository
public class SubmissionValuesJdbcDao implements ISubmissionValuesDao {

    private final NamedParameterJdbcTemplate jdbc;

    public SubmissionValuesJdbcDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // A single, unified UPSERT statement handles all cases.
    private static final String UPSERT_SQL = """
        INSERT INTO element_data_value (
            submission_id, assignment_id, team_id, org_unit_id, activity_id,
            element_id, element_label, repeat_instance_id, option_id,
            value_text, value_num, value_bool, value_ts, row_type, created_date, last_modified_date, deleted_at
        ) VALUES (
            :submissionId, :assignmentId, :teamId, :orgUnitId, :activityId,
            :elementId, :elementLabel, :repeatInstanceId, :optionId,
            :valueText, :valueNum, :valueBool, :valueTs, :rowType, :createdDate, :lastModifiedDate, NULL
        )
        ON CONFLICT (submission_id, element_id, repeat_instance_key, row_type, selection_key)
        DO UPDATE SET
            assignment_id = EXCLUDED.assignment_id,
            team_id = EXCLUDED.team_id,
            org_unit_id = EXCLUDED.org_unit_id,
            activity_id = EXCLUDED.activity_id,
            element_label = EXCLUDED.element_label,
            value_text = EXCLUDED.value_text,
            value_num = EXCLUDED.value_num,
            value_bool = EXCLUDED.value_bool,
            value_ts = EXCLUDED.value_ts,
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
    public void markAllAsDeletedForSubmission(String submissionId) {
        String sql = "UPDATE element_data_value SET deleted_at = now(), last_modified_date = now() " +
            "WHERE submission_id = :submissionId AND deleted_at IS NULL";
        jdbc.update(sql, new MapSqlParameterSource("submissionId", submissionId));
    }

    private MapSqlParameterSource toParamSource(ElementDataValue r) {
        // Ensure timestamps are set for new rows
        if (r.getCreatedDate() == null) r.setCreatedDate(Instant.now());
        if (r.getLastModifiedDate() == null) r.setLastModifiedDate(Instant.now());

        return new MapSqlParameterSource()
            .addValue("submissionId", r.getSubmissionId())
            .addValue("assignmentId", r.getAssignmentId())
            .addValue("teamId", r.getTeamId())
            .addValue("orgUnitId", r.getOrgUnitId())
            .addValue("activityId", r.getActivityId())
            .addValue("elementId", r.getElementId())
            .addValue("elementLabel", toJsonbObject(r.getElementLabel()), Types.OTHER)
            .addValue("repeatInstanceId", r.getRepeatInstanceId())
            .addValue("optionId", r.getOptionId())
            .addValue("valueText", r.getValueText())
            .addValue("valueNum", r.getValueNum())
            .addValue("valueBool", r.getValueBool())
            .addValue("valueTs", r.getValueTs() != null ? Timestamp.from(r.getValueTs()) : null)
            .addValue("rowType", r.getOptionId() != null ? "M" : "S") // Set row_type dynamically
            .addValue("createdDate", Timestamp.from(r.getCreatedDate()))
            .addValue("lastModifiedDate", Timestamp.from(r.getLastModifiedDate()));
    }

    private Object toJsonbObject(String json) {
        if (json == null) return null;
        try {
            org.postgresql.util.PGobject pg = new org.postgresql.util.PGobject();
            pg.setType("jsonb");
            pg.setValue(json);
            return pg;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert label to jsonb", e);
        }
    }
}
