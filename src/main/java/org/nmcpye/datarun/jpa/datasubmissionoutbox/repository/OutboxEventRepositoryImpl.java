package org.nmcpye.datarun.jpa.datasubmissionoutbox.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;

import java.time.Instant;
import java.util.List;

/**
 * - The WITH cte … FOR UPDATE SKIP LOCKED LIMIT :limit prevents multiple relay instances from double-claiming.
 * <p>
 * - UPDATE … RETURNING commits the claim and returns mapped entities in one round-trip.
 * <p>
 * - Attempts are incremented at claim time (so we can differentiate “never tried” vs “in-flight”).
 * <p>
 * - Markers keep a full audit trail (attempts, last error, timestamps).
 *
 * @author Hamza Assada
 * @since 15/08/2025
 */
public class OutboxEventRepositoryImpl implements OutboxEventClaimsRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Claim a batch atomically:
     * <p>
     * 1) Select candidate ids with SKIP LOCKED to avoid contention across relays
     * <p>
     * 2) Update them to PROCESSING and attempts = attempts + 1
     * <p>
     * 3) RETURNING full rows mapped to OutboxEvent entities
     */
    @Override
    public List<OutboxEvent> claimBatch(int limit) {
        String sql = """
            WITH cte AS (
                SELECT id
                FROM outbox_event
                WHERE status = 'PENDING'
                  AND available_at <= now()
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT :limit
            )
            UPDATE outbox_event o
            SET status = 'PROCESSING',
                attempts = o.attempts + 1
            FROM cte
            WHERE o.id = cte.id
            RETURNING o.*
            """;

        Query q = em.createNativeQuery(sql, OutboxEvent.class);
        q.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<OutboxEvent> rows = q.getResultList();
        return rows;
    }

    @Override
    public List<OutboxEvent> claimBatch(int limit, String eventType, String owner) {
        String sql = """
            WITH cte AS (
                SELECT id
                  FROM outbox_event
                  WHERE event_type = :eventType
                    AND status = 'PENDING'
                    AND available_at <= now()
                  ORDER BY created_at
                  FOR UPDATE SKIP LOCKED
                  LIMIT :limit
            )
            UPDATE outbox_event o
            SET status = 'PROCESSING',
                attempts = o.attempts + 1,
                processing_owner = :owner,
                processing_started_at = now()
            FROM cte
            WHERE o.id = cte.id
            RETURNING o.*;
            """;

        Query q = em.createNativeQuery(sql, OutboxEvent.class)
            .setParameter("limit", limit)
            .setParameter("eventType", eventType)
            .setParameter("owner", owner);

        @SuppressWarnings("unchecked")
        List<OutboxEvent> rows = q.getResultList();
        return rows;
    }

    @Override
    public void markDone(long id, Instant processedAt) {
        String sql = """
            UPDATE outbox_event
               SET status = 'DONE', processed_at = now(),
                   processing_owner = NULL, processing_started_at = NULL
               WHERE id = :id;
            """;
        em.createNativeQuery(sql)
            .setParameter("id", id)
            .executeUpdate();
    }

    @Override
    public void reschedule(long id, String errorMessage, Instant nextAvailableAt) {
        String sql = """

            UPDATE outbox_event
               SET status = 'PENDING',
                   available_at = :nextAvailableAt,
                   last_error = :error,
                   processing_owner = NULL,
                   processing_started_at = NULL
               WHERE id = :id;
            """;
        em.createNativeQuery(sql)
            .setParameter("nextAvailableAt", nextAvailableAt)
            .setParameter("error", truncate(errorMessage, 2000))
            .setParameter("id", id)
            .executeUpdate();
    }

    @Override
    public void markDlq(long id, String errorMessage) {
        String sql = """
            UPDATE outbox_event
               SET status = 'DLQ', last_error = :errorMessage,
                   processing_owner = NULL, processing_started_at = NULL
               WHERE id = :id;
            """;
        em.createNativeQuery(sql)
            .setParameter("errorMessage", truncate(errorMessage, 2000))
            .setParameter("id", id)
            .executeUpdate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> reclaimStale(int visibilitySeconds) {
        String sql = """
        UPDATE outbox_event
           SET status = 'PENDING',
               processing_owner = NULL,
               processing_started_at = NULL,
               available_at = now() + interval '30 seconds'
         WHERE status = 'PROCESSING'
           AND processing_started_at < now() - (:visibilitySeconds || ' seconds')::interval
        RETURNING id
        """;

        Query q = em.createNativeQuery(sql);
        q.setParameter("visibilitySeconds", visibilitySeconds);

        @SuppressWarnings("unchecked")
        List<Object> raw = q.getResultList();
        // result elements are usually BigInteger (Postgres numeric), convert to Long
        List<Long> ids = new java.util.ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o instanceof Number) {
                ids.add(((Number) o).longValue());
            } else {
                // defensive fallback
                ids.add(Long.parseLong(o.toString()));
            }
        }
        return ids;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
