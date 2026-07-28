package org.nmcpye.datarun.assignmentshadow;

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
public class JdbcAssignmentIdentityLink implements AssignmentIdentityLinkPort {

    private static final String SELECT_COLUMNS = """
        assignment_id,
        baseline_assignment_uid,
        target_actor_id,
        generation
        """;

    private static final String INSERT_SQL = """
        INSERT INTO assignment_identity_link (
            assignment_id,
            baseline_assignment_uid,
            target_actor_id,
            generation
        )
        VALUES (
            :assignmentId,
            :baselineAssignmentUid,
            :targetActorId,
            :generation
        )
        RETURNING
        """ + SELECT_COLUMNS;

    private static final String FIND_BY_ASSIGNMENT_ID_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM assignment_identity_link
        WHERE assignment_id = :assignmentId
        """;

    private static final String FIND_GENERATIONS_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM assignment_identity_link
        WHERE baseline_assignment_uid = :baselineAssignmentUid
          AND target_actor_id = :targetActorId
        ORDER BY generation
        """;

    private static final RowMapper<AssignmentIdentityLink> ROW_MAPPER = JdbcAssignmentIdentityLink::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAssignmentIdentityLink(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public AssignmentIdentityLink insert(AssignmentIdentityLink identityLink) {
        return jdbc.queryForObject(
            INSERT_SQL,
            new MapSqlParameterSource()
                .addValue("assignmentId", identityLink.assignmentId())
                .addValue("baselineAssignmentUid", identityLink.baselineAssignmentUid())
                .addValue("targetActorId", identityLink.targetActorId())
                .addValue("generation", identityLink.generation()),
            ROW_MAPPER
        );
    }

    @Override
    public Optional<AssignmentIdentityLink> findByAssignmentId(UUID assignmentId) {
        return jdbc.query(
            FIND_BY_ASSIGNMENT_ID_SQL,
            new MapSqlParameterSource("assignmentId", assignmentId),
            ROW_MAPPER
        ).stream().findFirst();
    }

    @Override
    public List<AssignmentIdentityLink> findGenerations(String baselineAssignmentUid, UUID targetActorId) {
        return jdbc.query(
            FIND_GENERATIONS_SQL,
            new MapSqlParameterSource()
                .addValue("baselineAssignmentUid", baselineAssignmentUid)
                .addValue("targetActorId", targetActorId),
            ROW_MAPPER
        );
    }

    private static AssignmentIdentityLink mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AssignmentIdentityLink(
            resultSet.getObject("assignment_id", UUID.class),
            resultSet.getString("baseline_assignment_uid"),
            resultSet.getObject("target_actor_id", UUID.class),
            resultSet.getInt("generation")
        );
    }
}
