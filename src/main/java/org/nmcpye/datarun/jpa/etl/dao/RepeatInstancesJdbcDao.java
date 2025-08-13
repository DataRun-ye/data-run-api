package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@SuppressWarnings("ConcatenationWithEmptyString")
@Repository
public class RepeatInstancesJdbcDao implements IRepeatInstancesDao {

    private final NamedParameterJdbcTemplate jdbc;

    public RepeatInstancesJdbcDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String UPSERT_SQL = ""
        + "INSERT INTO repeat_instance (id, submission_id, repeat_path, repeat_index, " +
        "client_updated_at, created_date, last_modified_date, created_by, last_modified_by, deleted_at) "
        + "VALUES (:id, :submission, :repeatPath, :repeatIndex, :clientUpdatedAt, :createdDate, :lastModifiedDate, :createdBy, :lastModifiedBy, :deletedAt) "
        + "ON CONFLICT (id) DO UPDATE SET "
        + "  submission_id = EXCLUDED.submission_id, "
        + "  repeat_path = EXCLUDED.repeat_path, "
        + "  repeat_index = EXCLUDED.repeat_index, "
        + "  client_updated_at = EXCLUDED.client_updated_at, "
        + "  last_modified_date = now(), "
        + "  last_modified_by = EXCLUDED.last_modified_by, "
        + "  deleted_at = EXCLUDED.deleted_at;";

    @Override
    public void upsertRepeatInstance(RepeatInstance repeatInstance) {
        Objects.requireNonNull(repeatInstance, "repeatInstance");
        if (repeatInstance.getCreatedDate() == null) repeatInstance.setCreatedDate(Instant.now());
        if (repeatInstance.getLastModifiedDate() == null) repeatInstance.setLastModifiedDate(Instant.now());
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", repeatInstance.getId())
            .addValue("submission", repeatInstance.getSubmission())
            .addValue("repeatPath", repeatInstance.getRepeatPath())
            .addValue("repeatIndex", repeatInstance.getRepeatIndex())
            .addValue("clientUpdatedAt", repeatInstance.getClientUpdatedAt())
            .addValue("createdDate", repeatInstance.getCreatedDate())
            .addValue("lastModifiedDate", repeatInstance.getLastModifiedDate())
            .addValue("createdBy", repeatInstance.getCreatedBy())
            .addValue("lastModifiedBy", repeatInstance.getLastModifiedBy())
            .addValue("deletedAt", null);

        // Ensure created/last dates exist
        if (repeatInstance.getLastModifiedDate() == null) {
            params.addValue("lastModifiedDate", new Date());
        }
        if (repeatInstance.getCreatedDate() == null) {
            params.addValue("createdDate", new Date());
        }

        jdbc.update(UPSERT_SQL, params);
    }

    @Override
    public void upsertRepeatInstancesBatch(List<RepeatInstance> batch) {
        if (batch == null || batch.isEmpty()) return;
        // ensure created/last dates are present
        List<SqlParameterSource> singleBatch = new ArrayList<>();
        batch.forEach(ri -> {
            if (ri.getCreatedDate() == null) ri.setCreatedDate(Instant.now());
            if (ri.getLastModifiedDate() == null) ri.setLastModifiedDate(Instant.now());
            // Create a MapSqlParameterSource for the current row
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", ri.getId())
                .addValue("submission", ri.getSubmission())
                .addValue("repeatPath", ri.getRepeatPath())
                .addValue("repeatIndex", ri.getRepeatIndex())
                .addValue("clientUpdatedAt", Timestamp.from(ri.getClientUpdatedAt() != null ? ri.getClientUpdatedAt() : Instant.now()))
                .addValue("createdDate", Timestamp.from(ri.getCreatedDate()))
                .addValue("lastModifiedDate", Timestamp.from(ri.getLastModifiedDate()))
                .addValue("createdBy", ri.getCreatedBy())
                .addValue("lastModifiedBy", ri.getLastModifiedBy())
                .addValue("deletedAt", null);
            singleBatch.add(params);
        });

        // Use BeanPropertySqlParameterSource batch
        jdbc.batchUpdate(UPSERT_SQL, SqlParameterSourceUtils.createBatch(singleBatch));
    }

    private static final String FIND_ACTIVE_UIDS_SQL = ""
        + "SELECT id FROM repeat_instance "
        + "WHERE submission_id = :submission AND repeat_path = :repeatPath AND deleted_at IS NULL";

    @Override
    public List<String> findActiveRepeatUids(String submission, String repeatPath) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submission)
            .addValue("repeatPath", repeatPath);
        return jdbc.query(FIND_ACTIVE_UIDS_SQL, params, (rs, rowNum) -> rs.getString("id"));
    }

    private static final String MARK_DELETED_SQL_BASE = ""
        + "UPDATE repeat_instance SET deleted_at = now(), last_modified_date = now() "
        + "WHERE submission_id = :submission AND repeat_path = :repeatPath AND id IN (:ids)";

    @Override
    public void markRepeatInstancesDeleted(String submission, String repeatPath, List<String> repeatUids) {
        if (repeatUids == null || repeatUids.isEmpty()) return;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submission)
            .addValue("repeatPath", repeatPath)
            .addValue("ids", repeatUids);
        jdbc.update(MARK_DELETED_SQL_BASE, params);
    }
}
