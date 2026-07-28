package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcAssignmentRoleDefinition implements AssignmentRoleDefinitionPort {

    private static final String SELECT_COLUMNS = """
        role_key,
        activity_uid,
        form_uids
        """;

    private static final String INSERT_SQL = """
        INSERT INTO assignment_role_definition (
            role_key,
            activity_uid,
            form_uids
        )
        VALUES (
            :roleKey,
            :activityUid,
            CAST(:formUids AS jsonb)
        )
        ON CONFLICT (role_key) DO NOTHING
        RETURNING
        """ + SELECT_COLUMNS;

    private static final String FIND_BY_ROLE_KEY_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM assignment_role_definition
        WHERE role_key = :roleKey
        """;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<AssignmentRoleDefinition> rowMapper = this::mapRow;

    public JdbcAssignmentRoleDefinition(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AssignmentRoleDefinition resolveOrInsert(String activityUid, List<String> formUids) {
        Objects.requireNonNull(activityUid, "activityUid");
        List<String> canonicalFormUids = canonicalize(formUids);
        if (canonicalFormUids.isEmpty()) {
            throw new IllegalArgumentException("Role form UID set cannot be empty");
        }
        String roleKey = roleKey(activityUid, canonicalFormUids);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("roleKey", roleKey)
            .addValue("activityUid", activityUid)
            .addValue("formUids", writeFormUids(canonicalFormUids));

        List<AssignmentRoleDefinition> inserted = jdbc.query(INSERT_SQL, parameters, rowMapper);
        if (!inserted.isEmpty()) {
            return inserted.get(0);
        }

        AssignmentRoleDefinition existing = findByRoleKey(roleKey)
            .orElseThrow(() -> new DataIntegrityViolationException(
                "Role key " + roleKey + " conflicted without a stored definition"
            ));
        if (!existing.activityUid().equals(activityUid) || !existing.formUids().equals(canonicalFormUids)) {
            throw new DataIntegrityViolationException(
                "Role key " + roleKey + " is already bound to different role content"
            );
        }
        return existing;
    }

    @Override
    public Optional<AssignmentRoleDefinition> findByRoleKey(String roleKey) {
        return jdbc.query(
            FIND_BY_ROLE_KEY_SQL,
            new MapSqlParameterSource("roleKey", roleKey),
            rowMapper
        ).stream().findFirst();
    }

    private List<String> canonicalize(List<String> formUids) {
        Objects.requireNonNull(formUids, "formUids");
        return formUids.stream()
            .map(formUid -> Objects.requireNonNull(formUid, "formUid"))
            .distinct()
            .sorted()
            .toList();
    }

    private String roleKey(String activityUid, List<String> canonicalFormUids) {
        byte[] canonicalBytes = writeFormUids(canonicalFormUids).getBytes(StandardCharsets.UTF_8);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonicalBytes);
            return "activity:" + activityUid + ":forms:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private AssignmentRoleDefinition mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AssignmentRoleDefinition(
            resultSet.getString("role_key"),
            resultSet.getString("activity_uid"),
            readFormUids(resultSet.getString("form_uids"))
        );
    }

    private String writeFormUids(List<String> formUids) {
        try {
            return objectMapper.writeValueAsString(formUids);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Role form UIDs cannot be serialized", exception);
        }
    }

    private List<String> readFormUids(String formUids) throws SQLException {
        try {
            return objectMapper.readValue(formUids, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Stored role form UIDs are not a JSON array", exception);
        }
    }
}
