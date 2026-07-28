package org.nmcpye.datarun.assignmentshadow;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcAssignmentGrantProjection implements AssignmentGrantProjectionPort {

    private static final String SELECT_COLUMNS = """
        assignment_id,
        source_event_id,
        role_key,
        org_unit_id,
        lifecycle_state
        """;

    private static final String INSERT_SQL = """
        INSERT INTO assignment_grant_projection (
            assignment_id,
            source_event_id,
            role_key,
            org_unit_id,
            lifecycle_state
        )
        VALUES (
            :assignmentId,
            :sourceEventId,
            :roleKey,
            :orgUnitId,
            :lifecycleState
        )
        RETURNING
        """ + SELECT_COLUMNS;

    private static final String UPDATE_SQL = """
        UPDATE assignment_grant_projection
        SET source_event_id = :sourceEventId,
            role_key = :roleKey,
            org_unit_id = :orgUnitId,
            lifecycle_state = :lifecycleState
        WHERE assignment_id = :assignmentId
          AND source_event_id = :expectedSourceEventId
        RETURNING
        """ + SELECT_COLUMNS;

    private static final String FIND_BY_ASSIGNMENT_ID_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM assignment_grant_projection
        WHERE assignment_id = :assignmentId
        """;

    private static final RowMapper<AssignmentGrantProjection> ROW_MAPPER = JdbcAssignmentGrantProjection::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    JdbcAssignmentGrantProjection(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public AssignmentGrantProjection insert(AssignmentGrantProjection projection) {
        return jdbc.queryForObject(INSERT_SQL, parameters(projection), ROW_MAPPER);
    }

    @Override
    @Transactional
    public AssignmentGrantProjection update(UUID expectedSourceEventId, AssignmentGrantProjection projection) {
        MapSqlParameterSource parameters = parameters(projection)
            .addValue("expectedSourceEventId", expectedSourceEventId);
        List<AssignmentGrantProjection> updated = jdbc.query(UPDATE_SQL, parameters, ROW_MAPPER);
        if (updated.isEmpty()) {
            throw new OptimisticLockingFailureException(
                "Assignment grant " + projection.assignmentId()
                    + " is no longer at source event " + expectedSourceEventId
            );
        }
        return updated.get(0);
    }

    @Override
    public Optional<AssignmentGrantProjection> findByAssignmentId(UUID assignmentId) {
        return jdbc.query(
            FIND_BY_ASSIGNMENT_ID_SQL,
            new MapSqlParameterSource("assignmentId", assignmentId),
            ROW_MAPPER
        ).stream().findFirst();
    }

    private static MapSqlParameterSource parameters(AssignmentGrantProjection projection) {
        return new MapSqlParameterSource()
            .addValue("assignmentId", projection.assignmentId())
            .addValue("sourceEventId", projection.sourceEventId())
            .addValue("roleKey", projection.roleKey())
            .addValue("orgUnitId", projection.orgUnitId())
            .addValue("lifecycleState", projection.lifecycleState().name());
    }

    private static AssignmentGrantProjection mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AssignmentGrantProjection(
            resultSet.getObject("assignment_id", UUID.class),
            resultSet.getObject("source_event_id", UUID.class),
            resultSet.getString("role_key"),
            resultSet.getObject("org_unit_id", UUID.class),
            AssignmentLifecycleState.valueOf(resultSet.getString("lifecycle_state"))
        );
    }
}
