package org.nmcpye.datarun.captureshadow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
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
    public void insertInitialPointer(UUID captureId, UUID sourceEventId) {
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
        return findSourceEventId(captureId, false);
    }

    @Override
    public Optional<UUID> findSourceEventIdForUpdate(UUID captureId) {
        return findSourceEventId(captureId, true);
    }

    @Override
    public void compareAndSwap(
        UUID captureId,
        UUID expectedSourceEventId,
        UUID newSourceEventId
    ) {
        int updated = jdbc.update(
            """
                UPDATE capture_current_projection
                SET source_event_id = ?
                WHERE capture_id = ?
                  AND source_event_id = ?
                """,
            newSourceEventId,
            captureId,
            expectedSourceEventId
        );
        if (updated != 1) {
            throw new OptimisticLockingFailureException(
                "Capture " + captureId
                    + " is no longer at source event "
                    + expectedSourceEventId
            );
        }
    }

    private Optional<UUID> findSourceEventId(
        UUID captureId,
        boolean forUpdate
    ) {
        List<UUID> eventIds = jdbc.query(
            """
                SELECT source_event_id
                FROM capture_current_projection
                WHERE capture_id = ?
                """ + (forUpdate ? " FOR UPDATE" : ""),
            (resultSet, rowNumber) -> resultSet.getObject("source_event_id", UUID.class),
            captureId
        );
        return eventIds.stream().findFirst();
    }
}
