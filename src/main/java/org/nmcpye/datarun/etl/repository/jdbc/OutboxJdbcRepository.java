package org.nmcpye.datarun.etl.repository.jdbc;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.nmcpye.datarun.etl.dto.OutboxDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository for operations which require Postgres-specific SQL (UPDATE ... RETURNING).
 * <p>
 * Responsibilities:
 * - Claim a batch of pending outbox rows by setting ingest_id (UPDATE ... WHERE status='pending' AND next_attempt_at <= now() LIMIT :n RETURNING *)
 * - Return claimed rows as OutboxDto
 * - Mark outbox rows success/failure with simple updates (could be via JPA in future)
 * <p>
 * NOTE: methods are stubs. Fill SQL and implement logic when ready.
 */
@SuppressWarnings("ConcatenationWithEmptyString")
@Repository
@RequiredArgsConstructor
@Slf4j
public class OutboxJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // ---------------------
    // SQL constants
    // ---------------------
    private static final String CLAIM_SQL =
        "WITH cte AS (" +
            "  SELECT outbox_id" +
            "  FROM outbox " +
            "  WHERE status = 'pending' " +
            "    AND (next_attempt_at IS NULL OR next_attempt_at <= now()) " +
            "  ORDER BY submission_serial_number " +
            "  FOR UPDATE SKIP LOCKED " +
            "  LIMIT :limit " +
            ") " +
            "UPDATE outbox " +
            "SET ingest_id = :ingestId, claimed_at= now(), claimed_by= :claimedBy " +
            "WHERE outbox_id IN (SELECT outbox_id FROM cte) " +
            "RETURNING outbox.*;";

    private static final String MARK_SUCCESS_SQL =
        "UPDATE outbox " +
            "SET status = 'success', last_error = NULL, next_attempt_at = NULL " +
            "WHERE outbox_id = :outboxId;";

    // If you prefer the 'RETURNING' form, use this:
    private static final String MARK_SUCCESS_SQL_RETURNING =
        "UPDATE outbox " +
            "SET status = 'success', last_error = NULL, next_attempt_at = NULL " +
            "WHERE outbox_id = :outboxId " +
            "RETURNING outbox_id;";

    private static final String MARK_FAILURE_SQL_RETURNING =
        "UPDATE outbox " +
            "SET attempt = COALESCE(attempt, 0) + 1, " +
            "    last_error = :error, " +
            "    next_attempt_at = :nextAttemptAt, " +
            "    status = 'failed' " +
            "WHERE outbox_id = :outboxId " +
            "RETURNING outbox_id, attempt;";

    // ---------------------
    // RowMapper
    // ---------------------
    private static final RowMapper<OutboxDto> OUTBOX_ROW_MAPPER = new RowMapper<>() {
        @Override
        public OutboxDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return OutboxDto.builder()
                .outboxId(rs.getLong("outbox_id"))
                .submissionSerialNumber(rs.getLong("submission_serial_number"))
                .submissionId(rs.getString("submission_id"))
                .submissionUid(rs.getString("submission_uid"))
                .eventType(rs.getString("event_type"))
                .topic(rs.getString("topic"))
                .payload(rs.getString("payload"))
                .status(rs.getString("status"))
                .attempt(rs.getInt("attempt"))
                .claimedBy(rs.getString("claimed_by"))
                .claimedAt(rs.getTimestamp("claimed_at") != null ? rs.getTimestamp("claimed_at").toInstant() : null)
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .ingestId(rs.getObject("ingest_id", UUID.class))
                .build();
        }
    };

    // ---------------------
    // Public API
    // ---------------------

    /**
     * Claim up to {@code limit} pending outbox rows and assign {@code ingestId} to them.
     * <p>
     * Concurrency & transactional notes:
     * - This method is annotated with @Transactional. The SELECT ... FOR UPDATE SKIP LOCKED
     * must run in a transaction to hold the row locks until the UPDATE executes.
     * - Use a reasonably small batch size (e.g., 100-1000) to avoid holding locks for long.
     *
     * @param ingestId UUID to assign to claimed rows
     * @param limit    max number of rows to claim
     * @return list of claimed OutboxDto rows (empty list if none available)
     */
    @Transactional
    public List<OutboxDto> claimPendingBatch(UUID ingestId, String claimedBy, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("claimedBy", claimedBy)
            .addValue("ingestId", ingestId)
            .addValue("limit", limit);

        // Execute the UPDATE ... RETURNING and map the returned rows to DTOs
        List<OutboxDto> claimed = jdbc.query(CLAIM_SQL, params, OUTBOX_ROW_MAPPER);

        return claimed;
    }

    /**
     * Mark the given outbox row as successfully processed.
     * <p>
     * This uses a simple UPDATE. If you want confirmation / the updated row back, use the
     * MARK_SUCCESS_SQL_RETURNING statement and jdbc.query(...) to read the returning row.
     *
     * @param outboxId target outbox id
     */
    @Transactional
    public void markOutboxSuccess(long outboxId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("outboxId", outboxId);

        // prefer simple update; check updated row count via jdbc.update
        int updated = jdbc.update(MARK_SUCCESS_SQL, params);
        if (updated == 0) {
            // Could be that row doesn't exist or was concurrently modified; caller should handle.
            throw new IllegalStateException("markOutboxSuccess: no outbox row updated for id=" + outboxId);
        }
    }

    /**
     * Mark the given outbox row as failed, record the error message and schedule next attempt.
     *
     * @param outboxId      target outbox id
     * @param error         short error message (be careful with length / sensitive info)
     * @param nextAttemptAt may be null to indicate immediate retry
     * @return the new attempt count (if returned), otherwise Optional.empty()
     */
    @Transactional
    public Optional<Integer> markOutboxFailure(long outboxId, String error, Instant nextAttemptAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("outboxId", outboxId)
            .addValue("error", error.substring(0, 4000))
            .addValue("nextAttemptAt", Timestamp.from(nextAttemptAt));

        // Use RETURNING to obtain the updated attempt count
        List<Map<String, Object>> rows = jdbc.queryForList(MARK_FAILURE_SQL_RETURNING, params);
        if (rows.isEmpty()) {
            // no row updated -> signal the caller
            throw new IllegalStateException("markOutboxFailure: no outbox row updated for id=" + outboxId);
        }
        Object attemptObj = rows.get(0).get("attempt");
        if (attemptObj == null) return Optional.empty();

        int attempt;
        if (attemptObj instanceof Number) {
            attempt = ((Number) attemptObj).intValue();
        } else {
            attempt = Integer.parseInt(attemptObj.toString());
        }
        return Optional.of(attempt);
    }

    // -------------
    // Helper convenience methods (optional)
    // -------------

    /**
     * Attempt to mark success using 'RETURNING' style and return true if a row was updated.
     * (Demonstrates how to use the RETURNING variant.)
     */
    @Transactional
    public boolean markOutboxSuccessReturning(long outboxId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("outboxId", outboxId);
        List<Long> ids = jdbc.query(MARK_SUCCESS_SQL_RETURNING, params, (rs, rowNum) -> rs.getLong("outbox_id"));
        return !ids.isEmpty();
    }

    /**
     * Batch insert backfill rows using ON CONFLICT DO NOTHING against uq_outbox_submission_backfill.
     * Returns number of inserted rows (count of batchUpdate results > 0).
     * <p>
     * Note: This method throws DataAccessException on DB errors so callers can retry/handle.
     */
    @Transactional
    public int insertBackfillIfNotExists(List<OutboxInsert> inserts) {
        if (inserts == null || inserts.isEmpty()) return 0;

        final String sql = ""
            + "INSERT INTO outbox ("
            + " submission_id, submission_uid, topic, payload, event_type, status, attempt, created_at, submission_serial_number"
            + ") VALUES ("
            + " :submission_id, :submission_uid, :topic, cast(:payload AS jsonb), 'BACKFILL', 'pending', 0, :created_at, :submission_serial_number"
            + ") ON CONFLICT (submission_id) WHERE (event_type = 'BACKFILL') DO NOTHING";

        MapSqlParameterSource[] batch = new MapSqlParameterSource[inserts.size()];
        for (int i = 0; i < inserts.size(); i++) {
            OutboxInsert it = inserts.get(i);
            MapSqlParameterSource p = new MapSqlParameterSource();
            p.addValue("submission_id", it.submissionId);
            p.addValue("submission_uid", it.submissionUid);
            p.addValue("topic", it.templateVersionUid);
            p.addValue("payload", it.payload);
            p.addValue("created_at", Timestamp.from(it.createdAt));
            p.addValue("submission_serial_number", it.submissionSerialNumber);
            batch[i] = p;
        }

        try {
            int[] results = jdbc.batchUpdate(sql, batch);
            int inserted = 0;
            for (int r : results) {
                if (r > 0) inserted++;
            }
            log.info("OutboxRepository: attempted {} inserts, {} inserted", inserts.size(), inserted);
            return inserted;
        } catch (DataAccessException dae) {
            log.error("OutboxRepository.insertBackfillIfNotExists failed: {}", dae.getMessage());
            throw dae;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public int insertIfNotExistsByEventType(List<OutboxInsert> inserts, String eventType) {
        if (inserts == null || inserts.isEmpty()) return 0;

        final String sql = ""
            + "INSERT INTO outbox ("
            + " submission_id, submission_serial_number, submission_uid, topic, payload, event_type, status, attempt, created_at"
            + ") VALUES ("
            + " :submission_id, :submission_serial_number, :submission_uid, :topic, cast(:payload AS jsonb), :event_type, 'pending', 0, :created_at"
            + ")";

        MapSqlParameterSource[] batch = new MapSqlParameterSource[inserts.size()];
        for (int i = 0; i < inserts.size(); i++) {
            OutboxInsert it = inserts.get(i);
            MapSqlParameterSource p = new MapSqlParameterSource();
            p.addValue("submission_serial_number", it.submissionSerialNumber);
            p.addValue("submission_id", it.submissionId);
            p.addValue("submission_uid", it.submissionUid);
            p.addValue("event_type", eventType);
            p.addValue("topic", it.templateVersionUid);
            p.addValue("payload", it.payload);
            p.addValue("created_at", Timestamp.from(it.createdAt));
            batch[i] = p;
        }

        try {
            int[] results = jdbc.batchUpdate(sql, batch);
            int inserted = 0;
            for (int r : results) {
                if (r > 0) inserted++;
            }
            log.info("OutboxRepository: attempted {} inserts, {} inserted", inserts.size(), inserted);
            return inserted;
        } catch (DataAccessException dae) {
            log.error("OutboxRepository.insertBackfillIfNotExists failed: {}", dae.getMessage());
            throw dae;
        }
    }

    /**
     * DTO used for binding outbox insert parameters.
     */
    public static class OutboxInsert {
        public final String submissionId;
        public final String submissionUid;
        public final String templateVersionUid;
        public final String payload;
        public final Instant createdAt;
        public final Long submissionSerialNumber;

        public OutboxInsert(String submissionId,
                            String submissionUid,
                            String templateVersionUid,
                            String payload,
                            Instant createdAt,
                            Long submissionSerialNumber) {
            this.submissionId = submissionId;
            this.submissionUid = submissionUid;
            this.templateVersionUid = templateVersionUid;
            this.payload = payload;
            this.createdAt = createdAt;
            this.submissionSerialNumber = submissionSerialNumber;
        }
    }
}
