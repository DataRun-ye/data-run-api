package org.nmcpye.datarun.jpa.datasubmissionoutbox.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEventStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;


/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Repository
public class OutboxAdminRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutboxAdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<OutboxEvent> ROW_MAPPER = new RowMapper<>() {
        @Override
        public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            OutboxEvent e = new OutboxEvent();
            e.setId(rs.getLong("id"));
            e.setAggregateType(rs.getString("aggregate_type"));
            e.setAggregateId(rs.getString("aggregate_id"));
            e.setEventType(rs.getString("event_type"));
            try {
                String payload = rs.getString("payload");
                e.setPayload(payload == null ? null : objectMapper.readTree(payload));
            } catch (Exception ex) {
                throw new SQLException("Failed to parse payload", ex);
            }
            e.setStatus(OutboxEventStatus.valueOf(rs.getString("status")));
            e.setAttempts(rs.getInt("attempts"));
            e.setLastError(rs.getString("last_error"));
            e.setCreatedAt(rs.getObject("created_at", Instant.class));
            e.setAvailableAt(rs.getObject("available_at", Instant.class));
            e.setProcessedAt(rs.getObject("processed_at", Instant.class));
            return e;
        }
    };


    public List<OutboxEvent> listByStatus(String status, int limit, int offset) {
        String sql = "SELECT id, aggregate_type, aggregate_id, event_type, payload, status, attempts, last_error, created_at, available_at, processed_at " +
            "FROM outbox_event WHERE status = ? ORDER BY created_at LIMIT ? OFFSET ?";
        return jdbc.query(sql, ROW_MAPPER, status, limit, offset);
    }


    public OutboxEvent findById(long id) {
        String sql = "SELECT id, aggregate_type, aggregate_id, event_type, payload, status, attempts, last_error, created_at, available_at, processed_at FROM outbox_event WHERE id = ?";
        List<OutboxEvent> list = jdbc.query(sql, ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public int requeue(long id) {
        String sql = "UPDATE outbox_event SET status='PENDING', attempts = 0, last_error = NULL, available_at = NULL WHERE id = ?";
        return jdbc.update(sql, id);
    }

    public int releaseClaim(long id) {
        String sql = "UPDATE outbox_event SET status='PENDING', available_at = now() + interval '30 seconds' WHERE id = ?";
        return jdbc.update(sql, id);
    }

    public int forceRetryNow(long id) {
        String sql = "UPDATE outbox_event SET available_at = now(), status='PENDING' WHERE id = ?";
        return jdbc.update(sql, id);
    }
}
