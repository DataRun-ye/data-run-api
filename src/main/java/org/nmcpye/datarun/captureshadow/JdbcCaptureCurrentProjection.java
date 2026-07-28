package org.nmcpye.datarun.captureshadow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCaptureCurrentProjection implements CaptureCurrentProjectionPort {

    private final JdbcTemplate jdbc;

    public JdbcCaptureCurrentProjection(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertBootstrapPointer(UUID captureId, UUID sourceEventId) {
        jdbc.update(
            """
                INSERT INTO capture_current_projection (capture_id, source_event_id)
                VALUES (?, ?)
                """,
            captureId,
            sourceEventId
        );
    }

    @Override
    public Optional<UUID> findSourceEventId(UUID captureId) {
        List<UUID> eventIds = jdbc.query(
            """
                SELECT source_event_id
                FROM capture_current_projection
                WHERE capture_id = ?
                """,
            (resultSet, rowNumber) -> resultSet.getObject("source_event_id", UUID.class),
            captureId
        );
        return eventIds.stream().findFirst();
    }
}
