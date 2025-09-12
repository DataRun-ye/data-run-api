package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

@Repository
public class RepeatInstancesJdbcDao implements IRepeatInstancesDao {

    private final NamedParameterJdbcTemplate jdbc;

    public RepeatInstancesJdbcDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // The UPSERT SQL handles "undeleting" via the ON CONFLICT clause (deleted_at = NULL in both places).
    private static final String UPSERT_SQL = """
        INSERT INTO repeat_instance ( id, semantic_path, submission_uid, repeat_path, parent_repeat_instance_id, repeat_index,
            client_updated_at, created_date, last_modified_date, created_by, last_modified_by,
            category_kind, category_uid, category_name, category_Label, repeat_section_label,
                                     submission_completed_at, deleted_at
        ) VALUES (
            :id, :semanticPath, :submissionUid, :repeatPath, :parentRepeatInstanceId, :repeatIndex,
            :clientUpdatedAt, :createdDate, :lastModifiedDate, :createdBy, :lastModifiedBy,
            :categoryKind, :categoryUid, :categoryName, :categoryLabel, :repeatSectionLabel, :submissionCompletedAt, NULL
        )
        ON CONFLICT (id) DO UPDATE SET
            repeat_path = EXCLUDED.repeat_path,
            parent_repeat_instance_id = EXCLUDED.parent_repeat_instance_id,
            repeat_index = EXCLUDED.repeat_index,
            client_updated_at = EXCLUDED.client_updated_at,
            repeat_section_label = EXCLUDED.repeat_section_label,
            category_kind = EXCLUDED.category_kind,
            category_uid = EXCLUDED.category_uid,
            category_name = EXCLUDED.category_name,
            category_label = EXCLUDED.category_label,
            submission_completed_at = EXCLUDED.submission_completed_at,
            last_modified_date = now(),
            last_modified_by = EXCLUDED.last_modified_by,
            deleted_at = NULL;
        """;

    @Override
    public void upsertRepeatInstancesBatch(List<RepeatInstance> batch) {
        if (batch == null || batch.isEmpty()) return;

        SqlParameterSource[] batchParams = batch.stream()
            .map(this::toParamSource)
            .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(UPSERT_SQL, batchParams);
    }

    @Override
    public void markAllAsDeletedForSubmission(String submissionUid) {
        String sql = "UPDATE repeat_instance SET deleted_at = now(), last_modified_date = now() " +
            "WHERE submission_uid = :submissionUid AND deleted_at IS NULL";
        jdbc.update(sql, new MapSqlParameterSource("submissionUid", submissionUid));
    }

    private MapSqlParameterSource toParamSource(RepeatInstance ri) {
        // Ensure timestamps are set for new instances
        if (ri.getCreatedDate() == null) ri.setCreatedDate(Instant.now());
        if (ri.getLastModifiedDate() == null) ri.setLastModifiedDate(Instant.now());

        return new MapSqlParameterSource()
            .addValue("id", ri.getId())
            .addValue("semanticPath", ri.getSemanticPath())
            .addValue("submissionUid", ri.getSubmissionUid())
            .addValue("repeatPath", ri.getRepeatPath())
            .addValue("parentRepeatInstanceId", ri.getParentRepeatInstanceId()) // New field
            .addValue("repeatIndex", ri.getRepeatIndex())

            .addValue("clientUpdatedAt", ri.getClientUpdatedAt() != null ?
                Timestamp.from(ri.getClientUpdatedAt()) : null)

            .addValue("submissionCompletedAt", ri.getSubmissionCompletedAt() != null ?
                Timestamp.from(ri.getSubmissionCompletedAt()) : null)

            .addValue("categoryKind", ri.getCategoryKind())
            .addValue("categoryUid", ri.getCategoryUid())
            .addValue("categoryName", ri.getCategoryName())
            .addValue("categoryLabel", toJsonbObject(ri.getCategoryLabel()), Types.OTHER)

            .addValue("repeatSectionLabel",
                toJsonbObject(ri.getRepeatSectionLabel()), Types.OTHER)

            .addValue("createdDate", Timestamp.from(ri.getCreatedDate()))
            .addValue("lastModifiedDate", Timestamp.from(ri.getLastModifiedDate()))
            .addValue("createdBy", ri.getCreatedBy())
            .addValue("lastModifiedBy", ri.getLastModifiedBy());
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
