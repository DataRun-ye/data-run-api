package org.nmcpye.datarun.transition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class TransitionCheckpointStore {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public TransitionCheckpointStore(
        NamedParameterJdbcTemplate jdbc,
        ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<TransitionCheckpoint> find(String key) {
        List<TransitionCheckpoint> values = jdbc.query(
            """
                SELECT checkpoint_key, recorded_at, payload::text AS payload
                FROM transition_checkpoint
                WHERE checkpoint_key = :key
                """,
            new MapSqlParameterSource("key", key),
            this::map
        );
        return values.stream().findFirst();
    }

    public void insert(String key, Instant recordedAt, JsonNode payload) {
        jdbc.update(
            """
                INSERT INTO transition_checkpoint (
                    checkpoint_key,
                    recorded_at,
                    payload
                ) VALUES (
                    :key,
                    :recordedAt,
                    CAST(:payload AS jsonb)
                )
                ON CONFLICT (checkpoint_key) DO NOTHING
                """,
            new MapSqlParameterSource()
                .addValue("key", key)
                .addValue("recordedAt", Timestamp.from(recordedAt))
                .addValue("payload", writeJson(payload))
        );
    }

    private TransitionCheckpoint map(
        ResultSet resultSet,
        int rowNumber
    ) throws SQLException {
        return new TransitionCheckpoint(
            resultSet.getString("checkpoint_key"),
            resultSet.getTimestamp("recorded_at").toInstant(),
            readJson(resultSet.getString("payload"))
        );
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                "Transition checkpoint payload cannot be serialized",
                exception
            );
        }
    }

    private JsonNode readJson(String value) throws SQLException {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new SQLException(
                "Stored transition checkpoint payload is invalid JSON",
                exception
            );
        }
    }
}
