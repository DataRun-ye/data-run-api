package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

    // Conflict target uses generated columns + row_type + selection_key
    private static final String UPSERT_SINGLE_SQL = """
        INSERT INTO element_data_value (
          submission_id, repeat_instance_id, element_id,
          value_text, value_num, value_bool,
          assignment_id, template_id, category_id,
          row_type, deleted_at, created_date, last_modified_date
        ) VALUES (
          :submission, :repeatInstance, :element,
          :valueText, :valueNum, :valueBool,
          :assignment, :template, :category,
          :rowType, :deletedAt, :createdDate, :lastModifiedDate
        )
        ON CONFLICT (submission_id, element_id, repeat_instance_key, row_type, selection_key)
        DO UPDATE SET
          value_text = EXCLUDED.value_text,
          value_num = EXCLUDED.value_num,
          value_bool = EXCLUDED.value_bool,
          category_id = EXCLUDED.category_id,
          deleted_at = NULL,
          last_modified_date = now();
        """;

    private static final String UPSERT_MULTI_SQL = """
        INSERT INTO element_data_value (
          submission_id, repeat_instance_id, element_id,
          option_id, value_text,
          assignment_id, template_id, category_id,
          row_type, deleted_at, created_date, last_modified_date
        ) VALUES (
          :submission, :repeatInstance, :element,
          :optionId, :valueText,
          :assignment, :template, :category,
          :rowType, :deletedAt, :createdDate, :lastModifiedDate
        )
        ON CONFLICT (submission_id, element_id, repeat_instance_key, row_type, selection_key)
        DO UPDATE SET
          value_text = EXCLUDED.value_text,
          category_id = EXCLUDED.category_id,
          deleted_at = NULL,
          last_modified_date = now();
        """;

    @Override
    public void upsertSubmissionValue(SubmissionValueRow r) {
        upsertSubmissionValuesBatch(Collections.singletonList(r));
    }

    @Override
    public void upsertSubmissionValuesBatch(List<SubmissionValueRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        final Instant now = Instant.now();

        // Partition rows into singles (optionId == null) and multis (optionId != null)
        List<MapSqlParameterSource> singleParams = new ArrayList<>();
        List<MapSqlParameterSource> multiParams = new ArrayList<>();

        for (SubmissionValueRow r : rows) {
            if (r.getCreatedDate() == null) r.setCreatedDate(now);
            if (r.getLastModifiedDate() == null) r.setLastModifiedDate(now);

            MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("submission", r.getSubmission())
                .addValue("repeatInstance", r.getRepeatInstance())
                .addValue("element", r.getElement())
                .addValue("valueText", r.getValueText())
                .addValue("valueNum", r.getValueNum())
                .addValue("valueBool", r.getValueBool())
                .addValue("assignment", r.getAssignment())
                .addValue("template", r.getTemplate())
                .addValue("category", r.getCategory())
                .addValue("createdDate", Timestamp.from(r.getCreatedDate()))
                .addValue("lastModifiedDate", Timestamp.from(r.getLastModifiedDate()))
                .addValue("deletedAt", r.getDeletedAt());

            if (r.getOption() != null) {
                // multi-select row: set optionId and row_type = 'M'
                p.addValue("optionId", r.getOption());
                p.addValue("rowType", "M");
                // optionally prefer to clear valueText if storing only optionId: keep whatever r provides
                multiParams.add(p);
            } else {
                // single-value row: row_type = 'S'
                p.addValue("rowType", "S");
                singleParams.add(p);
            }
        }

        if (!singleParams.isEmpty()) {
            SqlParameterSource[] batch = singleParams.toArray(new SqlParameterSource[0]);
            jdbc.batchUpdate(UPSERT_SINGLE_SQL, batch);
        }
        if (!multiParams.isEmpty()) {
            SqlParameterSource[] batch = multiParams.toArray(new SqlParameterSource[0]);
            jdbc.batchUpdate(UPSERT_MULTI_SQL, batch);
        }
    }

    @Override
    public List<String> findSelectionIdentitiesForElementRepeat(String submissionId, String repeatInstanceId, String elementId) {
        String sql;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submissionId)
            .addValue("element", elementId);

        if (repeatInstanceId == null) {
            sql = """
                SELECT COALESCE(option_id, value_text) AS identity
                FROM element_data_value
                WHERE submission_id = :submission
                  AND element_id = :element
                  AND deleted_at IS NULL
                  AND repeat_instance_id IS NULL
                """;
        } else {
            sql = """
                SELECT COALESCE(option_id, value_text) AS identity
                FROM element_data_value
                WHERE submission_id = :submission
                  AND element_id = :element
                  AND deleted_at IS NULL
                  AND repeat_instance_id = :repeatInstance
                """;
            params.addValue("repeatInstance", repeatInstanceId);
        }

        List<String> list = jdbc.queryForList(sql, params, String.class);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void markSelectionValuesDeletedByIdentity(String submissionId, String repeatInstanceId, String elementId, List<String> identities) {
        if (identities == null || identities.isEmpty()) return;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submissionId)
            .addValue("element", elementId)
            .addValue("idents", identities);

        String sql;
        if (repeatInstanceId == null) {
            sql = """
                UPDATE element_data_value
                SET deleted_at = now(), last_modified_date = now()
                WHERE submission_id = :submission
                  AND element_id = :element
                  AND COALESCE(option_id, value_text) IN (:idents)
                  AND repeat_instance_id IS NULL
                """;
        } else {
            sql = """
                UPDATE element_data_value
                SET deleted_at = now(), last_modified_date = now()
                WHERE submission_id = :submission
                  AND element_id = :element
                  AND COALESCE(option_id, value_text) IN (:idents)
                  AND repeat_instance_id = :repeatInstance
                """;
            params.addValue("repeatInstance", repeatInstanceId);
        }

        jdbc.update(sql, params);
    }

    @Override
    public void markValuesDeletedForRepeatUids(String submissionId, List<String> repeatUids) {
        if (repeatUids == null || repeatUids.isEmpty()) return;
        String sql = "UPDATE element_data_value SET deleted_at = now(), last_modified_date = now() WHERE submission_id = :submission AND repeat_instance_id IN (:uids)";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submissionId)
            .addValue("uids", repeatUids);
        jdbc.update(sql, params);
    }

    @Override
    public void markValuesDeletedForSubmission(String submissionId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("submission", submissionId);
        jdbc.update("UPDATE element_data_value SET deleted_at = now(), last_modified_date = now() " +
            "WHERE submission_id = :submission AND deleted_at IS NULL", params);
    }
}
