package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Repository
@SuppressWarnings("ConcatenationWithEmptyString")
public class RepeatInstancesJdbcDao implements IRepeatInstancesDao {

    private final NamedParameterJdbcTemplate jdbc;

    public RepeatInstancesJdbcDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String UPSERT_SQL = ""
        + "INSERT INTO repeat_instance (id, submission_id, repeat_path, repeat_index, "
        + "client_updated_at, created_date, last_modified_date, created_by, last_modified_by, deleted_at) "
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

        MapSqlParameterSource params = toParamSource(repeatInstance);
        jdbc.update(UPSERT_SQL, params);
    }

    @Override
    public void upsertRepeatInstancesBatch(List<RepeatInstance> batch) {
        if (batch == null || batch.isEmpty()) return;

        // Ensure timestamps exist and build parameter sources
        List<MapSqlParameterSource> paramsList = new ArrayList<>(batch.size());
        for (RepeatInstance ri : batch) {
            if (ri.getCreatedDate() == null) ri.setCreatedDate(Instant.now());
            if (ri.getLastModifiedDate() == null) ri.setLastModifiedDate(Instant.now());
            paramsList.add(toParamSource(ri));
        }

        SqlParameterSource[] batchParams = paramsList.toArray(new SqlParameterSource[0]);
        jdbc.batchUpdate(UPSERT_SQL, batchParams);
    }

    private MapSqlParameterSource toParamSource(RepeatInstance ri) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("id", ri.getId());
        p.addValue("submission", ri.getSubmission());
        p.addValue("repeatPath", ri.getRepeatPath());
        p.addValue("repeatIndex", ri.getRepeatIndex());
        if (ri.getClientUpdatedAt() != null) {
            p.addValue("clientUpdatedAt", Timestamp.from(ri.getClientUpdatedAt()));
        } else {
            p.addValue("clientUpdatedAt", null);
        }
        p.addValue("createdDate", ri.getCreatedDate() != null ? Timestamp.from(ri.getCreatedDate()) : Timestamp.from(Instant.now()));
        p.addValue("lastModifiedDate", ri.getLastModifiedDate() != null ? Timestamp.from(ri.getLastModifiedDate()) : Timestamp.from(Instant.now()));
        p.addValue("createdBy", ri.getCreatedBy());
        p.addValue("lastModifiedBy", ri.getLastModifiedBy());
        p.addValue("deletedAt", ri.getDeletedAt() != null ? Timestamp.from(ri.getDeletedAt()) : null);
        return p;
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

    /**
     * Convenience: mark all repeat instances for a submission deleted (used for soft-delete cascade).
     */
    @Override
    public void markRepeatInstancesDeletedBySubmission(String submissionId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("submission", submissionId);
        jdbc.update("UPDATE repeat_instance SET deleted_at = now(), last_modified_date = now() WHERE submission_id = :submission AND deleted_at IS NULL", params);
    }
}
