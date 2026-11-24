package org.nmcpye.datarun.outbox.repository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class OutboxWriter implements OutboxWritePort {

    /** created_at is left to its default: the database's transaction time, in UTC. */
    private static final String INSERT_SQL = """
        INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload, event_type,
                            status, attempt, submission_version)
        VALUES (:submission_id, :submission_serial_number, :submission_uid, :topic, cast(:payload AS jsonb),
                :event_type, 'pending', 0, :submission_version)
        """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    @Transactional
    public int insert(List<OutboxInsert> inserts) {
        if (inserts == null || inserts.isEmpty()) return 0;

        MapSqlParameterSource[] batch = inserts.stream().map(it -> new MapSqlParameterSource()
            .addValue("submission_id", it.submissionId())
            .addValue("submission_uid", it.submissionUid())
            .addValue("submission_serial_number", it.submissionSerialNumber())
            .addValue("topic", it.topic())
            .addValue("payload", it.payload())
            .addValue("event_type", it.eventType())
            .addValue("submission_version", it.submissionVersion())
        ).toArray(MapSqlParameterSource[]::new);

        int[] results = jdbc.batchUpdate(INSERT_SQL, batch);
        int inserted = (int) Arrays.stream(results).filter(r -> r > 0).count();
        log.info("OutboxWriter: attempted {}, inserted {}", inserts.size(), inserted);
        return inserted;
    }
}
