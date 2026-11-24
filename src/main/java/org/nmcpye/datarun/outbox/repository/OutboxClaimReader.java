package org.nmcpye.datarun.outbox.repository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class OutboxClaimReader implements OutboxClaimPort {

    private final NamedParameterJdbcTemplate cloudJdbc;

    /**
     * Claims the next rows that are due, oldest submission first and outbox id on a tie, and leases them:
     * {@code next_attempt_at} moves to the end of the lease, so no other claim takes them meanwhile and a run
     * that dies leaves them to be claimed again afterwards. A claim is not an attempt: a row's attempt is counted
     * when its processing starts, so rows a dying batch never reached lose nothing.
     */
    private static final String CLAIM_SQL = """
        WITH due AS (
          SELECT outbox_id
          FROM outbox
          WHERE status IN ('pending', 'failed')
            AND (next_attempt_at IS NULL OR next_attempt_at <= now())
          ORDER BY submission_serial_number, outbox_id
          LIMIT :limit
          FOR UPDATE SKIP LOCKED
        )
        UPDATE outbox o
        SET ingest_id = :ingestId,
            claimed_at = now(),
            claimed_by = :claimedBy,
            next_attempt_at = now() + make_interval(secs => :leaseSeconds)
        FROM due
        WHERE o.outbox_id = due.outbox_id
        RETURNING o.*
        """;

    private static final Comparator<OutboxDto> CLAIM_ORDER = Comparator
        .comparing(OutboxDto::getSubmissionSerialNumber)
        .thenComparing(OutboxDto::getOutboxId);

    /** Counts an attempt as the row's processing starts, for the claim that holds it. */
    private static final String START_ATTEMPT_SQL = """
        UPDATE outbox SET attempt = COALESCE(attempt, 0) + 1
        WHERE outbox_id = :outboxId AND ingest_id = :ingestId
        RETURNING attempt
        """;

    /** Ends the lease of rows a stopped batch did not finish, so the next run can take them at once. */
    private static final String RELEASE_SQL = """
        UPDATE outbox SET next_attempt_at = NULL
        WHERE ingest_id = :ingestId AND outbox_id IN (:outboxIds) AND status IN ('pending', 'failed')
        """;

    private static final String MARK_SUCCESS_SQL =
        "UPDATE outbox " +
            "SET status = 'success', last_error = NULL, next_attempt_at = NULL " +
            "WHERE outbox_id = :outboxId;";

    // Failure outcomes apply only to the claim they belong to: a row claimed again after its lease ended
    // belongs to the newer claim.
    private static final String MARK_RETRY_SQL = """
        UPDATE outbox
        SET status = 'failed',
            last_error = :error,
            next_attempt_at = now() + make_interval(secs => :retrySeconds)
        WHERE outbox_id = :outboxId AND ingest_id = :ingestId
        """;

    private static final String MARK_DEAD_SQL = """
        UPDATE outbox
        SET status = 'dead',
            last_error = :error,
            next_attempt_at = NULL
        WHERE outbox_id = :outboxId AND ingest_id = :ingestId
        """;

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

    @Override
    @Transactional
    public List<OutboxDto> claimPendingBatch(UUID ingestId, String claimedBy, int limit, Duration lease) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("claimedBy", claimedBy)
            .addValue("ingestId", ingestId)
            .addValue("limit", limit)
            .addValue("leaseSeconds", (double) lease.toSeconds());

        List<OutboxDto> claimed = new ArrayList<>(cloudJdbc.query(CLAIM_SQL, params, OUTBOX_ROW_MAPPER));
        // UPDATE ... RETURNING does not keep the claim order.
        claimed.sort(CLAIM_ORDER);
        return claimed;
    }

    @Override
    @Transactional
    public OptionalInt startAttempt(long outboxId, UUID ingestId) {
        List<Integer> attempts = cloudJdbc.query(START_ATTEMPT_SQL,
            new MapSqlParameterSource().addValue("outboxId", outboxId).addValue("ingestId", ingestId),
            (rs, rowNum) -> rs.getInt("attempt"));
        return attempts.isEmpty() ? OptionalInt.empty() : OptionalInt.of(attempts.get(0));
    }

    @Override
    @Transactional
    public int release(UUID ingestId, Collection<Long> outboxIds) {
        if (outboxIds.isEmpty()) {
            return 0;
        }
        return cloudJdbc.update(RELEASE_SQL,
            new MapSqlParameterSource().addValue("ingestId", ingestId).addValue("outboxIds", outboxIds));
    }

    // --- claim processor

    @Override
    @Transactional
    public void markSuccess(long outboxId) {
        int updated = cloudJdbc.update(MARK_SUCCESS_SQL, new MapSqlParameterSource("outboxId", outboxId));
        if (updated == 0) {
            throw new IllegalStateException("markSuccess: no outbox row updated for id=" + outboxId);
        }
    }

    @Override
    @Transactional
    public boolean markRetry(long outboxId, UUID ingestId, String error, Duration retryAfter) {
        return cloudJdbc.update(MARK_RETRY_SQL, failure(outboxId, ingestId, error)
            .addValue("retrySeconds", (double) retryAfter.toSeconds())) > 0;
    }

    @Override
    @Transactional
    public boolean markDead(long outboxId, UUID ingestId, String error) {
        return cloudJdbc.update(MARK_DEAD_SQL, failure(outboxId, ingestId, error)) > 0;
    }

    private static MapSqlParameterSource failure(long outboxId, UUID ingestId, String error) {
        return new MapSqlParameterSource()
            .addValue("outboxId", outboxId)
            .addValue("ingestId", ingestId)
            .addValue("error", error == null ? null : error.substring(0, Math.min(4000, error.length())));
    }
}

