package org.nmcpye.datarun.jpa.datasubmissionoutbox.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
@Repository
@Transactional
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
    public void markDone(long id, Instant processedAt) {
        String sql = """
            UPDATE outbox_event
               SET status = 'DONE',
                   processed_at = :processedAt
             WHERE id = :id
            """;
        em.createNativeQuery(sql)
            .setParameter("processedAt", processedAt)
            .setParameter("id", id)
            .executeUpdate();
    }

    @Override
    public void reschedule(long id, String errorMessage, Instant nextAvailableAt) {
        String sql = """
            UPDATE outbox_event
               SET status = 'PENDING',
                   available_at = :nextAvailableAt,
                   last_error = :error
             WHERE id = :id
            """;
        em.createNativeQuery(sql)
            .setParameter("nextAvailableAt", nextAvailableAt)
            .setParameter("error", truncate(errorMessage, 2000))
            .setParameter("id", id)
            .executeUpdate();
    }

    @Override
    public void markFailed(long id, String errorMessage) {
        String sql = """
            UPDATE outbox_event
               SET status = 'FAILED',
                   last_error = :error
             WHERE id = :id
            """;
        em.createNativeQuery(sql)
            .setParameter("error", truncate(errorMessage, 2000))
            .setParameter("id", id)
            .executeUpdate();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
