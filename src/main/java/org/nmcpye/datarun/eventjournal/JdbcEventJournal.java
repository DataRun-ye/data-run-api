package org.nmcpye.datarun.eventjournal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcEventJournal implements EventJournalPort {

    private static final String SELECT_COLUMNS = """
        journal_position,
        event_id,
        event_type,
        shape_ref,
        activity_ref,
        subject_type,
        subject_id,
        actor_id,
        recorded_at,
        payload
        """;

    private static final String APPEND_SQL = """
        INSERT INTO event_journal (
            event_id,
            event_type,
            shape_ref,
            activity_ref,
            subject_type,
            subject_id,
            actor_id,
            recorded_at,
            payload
        )
        VALUES (
            :eventId,
            :eventType,
            :shapeRef,
            :activityRef,
            :subjectType,
            :subjectId,
            :actorId,
            :recordedAt,
            CAST(:payload AS jsonb)
        )
        RETURNING
        """ + SELECT_COLUMNS;

    private static final String FIND_BY_EVENT_ID_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM event_journal
        WHERE event_id = :eventId
        """;

    private static final String FIND_BY_SUBJECT_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM event_journal
        WHERE subject_type = :subjectType
          AND subject_id = :subjectId
        ORDER BY journal_position
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<JournalEvent> rowMapper = this::mapRow;

    public JdbcEventJournal(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public JournalEvent append(AppendJournalEvent event) {
        EventContract.requireValid(event);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("eventId", event.eventId())
            .addValue("eventType", event.eventType())
            .addValue("shapeRef", event.shapeRef())
            .addValue("activityRef", event.activityRef())
            .addValue("subjectType", event.subjectType())
            .addValue("subjectId", event.subjectId())
            .addValue("actorId", event.actorId())
            .addValue("recordedAt", Timestamp.from(event.recordedAt()))
            .addValue("payload", writeJson(event.payload()));

        return jdbc.queryForObject(APPEND_SQL, parameters, rowMapper);
    }

    @Override
    public Optional<JournalEvent> findByEventId(UUID eventId) {
        List<JournalEvent> events = jdbc.query(
            FIND_BY_EVENT_ID_SQL,
            new MapSqlParameterSource("eventId", eventId),
            rowMapper
        );
        return events.stream().findFirst();
    }

    @Override
    public List<JournalEvent> findBySubject(String subjectType, UUID subjectId) {
        return jdbc.query(
            FIND_BY_SUBJECT_SQL,
            new MapSqlParameterSource()
                .addValue("subjectType", subjectType)
                .addValue("subjectId", subjectId),
            rowMapper
        );
    }

    private JournalEvent mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new JournalEvent(
            resultSet.getLong("journal_position"),
            resultSet.getObject("event_id", UUID.class),
            resultSet.getString("event_type"),
            resultSet.getString("shape_ref"),
            resultSet.getString("activity_ref"),
            resultSet.getString("subject_type"),
            resultSet.getObject("subject_id", UUID.class),
            resultSet.getString("actor_id"),
            resultSet.getTimestamp("recorded_at").toInstant(),
            readJson(resultSet.getString("payload"))
        );
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Journal payload cannot be serialized", exception);
        }
    }

    private JsonNode readJson(String value) throws SQLException {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Stored journal payload is not valid JSON", exception);
        }
    }
}
