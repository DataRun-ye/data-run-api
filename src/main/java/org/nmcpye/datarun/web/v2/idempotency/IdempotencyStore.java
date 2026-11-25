package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.SqlStates;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * The idempotency record, {@code api.idempotency_key}, by SQL in the caller's transaction (the command's own).
 * <p>
 * A claim is an insert that waits on the primary key for a concurrent claim of the same key: when that one commits,
 * this one finds its completed row; when it rolls back, this one claims. A row past its expiry counts as absent
 * and is claimed in place. The wait is bounded by the transaction's {@code lock_timeout}
 * ({@link #limitLockWaits}); a claim still waiting then is 409 {@code request-in-progress}.
 * <p>
 * The SQL relies on READ COMMITTED, where each statement sees what committed before it started.
 */
@Repository
public class IdempotencyStore {

    /** The result of a claim. */
    public sealed interface Claim permits Claimed, Completed {
    }

    /** The key is this transaction's now; the command runs. */
    public record Claimed() implements Claim {
    }

    /**
     * The key belongs to a committed command: its fingerprint and outcome. The row stays locked by this transaction
     * until it ends, so no one claims it meanwhile.
     */
    public record Completed(byte[] requestHash, CommandOutcome outcome) implements Claim {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public IdempotencyStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Bounds every lock wait for the rest of the current transaction ({@code SET LOCAL lock_timeout}). */
    public void limitLockWaits(Duration timeout) {
        jdbc.queryForObject("SELECT set_config('lock_timeout', :timeout, true)",
            new MapSqlParameterSource("timeout", Math.max(1, timeout.toMillis()) + "ms"), String.class);
    }

    /**
     * @throws ApiProblemException 409 {@code request-in-progress} when a concurrent claim of the key holds it
     *                             past the lock timeout
     */
    public Claim claim(String actorUserId, UUID key, byte[] requestHash, Duration retention) {
        MapSqlParameterSource row = new MapSqlParameterSource()
            .addValue("actor", actorUserId)
            .addValue("key", key)
            .addValue("hash", requestHash)
            .addValue("retention", retention.toSeconds());
        List<Integer> claimed;
        try {
            claimed = jdbc.queryForList("""
                INSERT INTO api.idempotency_key (actor_user_id, idempotency_key, request_hash, created_at, expires_at)
                VALUES (:actor, :key, :hash, now(), now() + make_interval(secs => :retention))
                ON CONFLICT (actor_user_id, idempotency_key) DO UPDATE
                    SET request_hash = EXCLUDED.request_hash,
                        created_at = EXCLUDED.created_at,
                        expires_at = EXCLUDED.expires_at,
                        resource_type = NULL, resource_id = NULL, resource_version = NULL, response_status = NULL
                    WHERE api.idempotency_key.expires_at <= now()
                RETURNING 1""", row, Integer.class);
        } catch (DataAccessException e) {
            if (e instanceof PessimisticLockingFailureException || SqlStates.has(e, SqlStates.LOCK_NOT_AVAILABLE)) {
                throw ApiProblemException.of(ProblemCode.REQUEST_IN_PROGRESS)
                    .detail("An earlier request with this Idempotency-Key is still running; send this one again "
                        + "shortly.")
                    .header(HttpHeaders.RETRY_AFTER, "1");
            }
            throw e;
        }
        if (!claimed.isEmpty()) {
            return new Claimed();
        }
        return jdbc.queryForObject("""
            SELECT request_hash, resource_type, resource_id, resource_version, response_status
            FROM api.idempotency_key
            WHERE actor_user_id = :actor AND idempotency_key = :key""", row, (rs, n) -> {
            String type = rs.getString("resource_type");
            CommandOutcome outcome = type == null ? null : new CommandOutcome(rs.getInt("response_status"), type,
                rs.getString("resource_id"), rs.getLong("resource_version"));
            return new Completed(rs.getBytes("request_hash"), outcome);
        });
    }

    /** Writes the outcome on the key this transaction claimed. */
    public void recordOutcome(String actorUserId, UUID key, CommandOutcome outcome) {
        int updated = jdbc.update("""
            UPDATE api.idempotency_key
            SET resource_type = :type, resource_id = :id, resource_version = :version, response_status = :status
            WHERE actor_user_id = :actor AND idempotency_key = :key AND response_status IS NULL""",
            new MapSqlParameterSource()
                .addValue("actor", actorUserId)
                .addValue("key", key)
                .addValue("type", outcome.resourceType())
                .addValue("id", outcome.resourceId())
                .addValue("version", outcome.resourceVersion())
                .addValue("status", outcome.responseStatus()));
        if (updated != 1) {
            throw new IllegalStateException("The idempotency key was not claimed by this transaction");
        }
    }

    /**
     * Deletes up to {@code batchSize} expired keys, skipping any a claim holds, and says how many it deleted.
     */
    public int purgeExpired(int batchSize) {
        return jdbc.update("""
            DELETE FROM api.idempotency_key
            WHERE (actor_user_id, idempotency_key) IN (
                SELECT actor_user_id, idempotency_key
                FROM api.idempotency_key
                WHERE expires_at <= now()
                ORDER BY expires_at
                LIMIT :batch
                FOR UPDATE SKIP LOCKED)""", new MapSqlParameterSource("batch", batchSize));
    }
}
