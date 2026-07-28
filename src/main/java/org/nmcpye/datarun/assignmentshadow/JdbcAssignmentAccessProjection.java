package org.nmcpye.datarun.assignmentshadow;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAssignmentAccessProjection implements AssignmentAccessProjectionPort {

    private static final String FIND_BY_TARGET_ACTOR_SQL = """
        SELECT
            target_actor_id,
            activity_uid,
            org_unit_id,
            role_key
        FROM assignment_access_projection
        WHERE target_actor_id = :targetActorId
        ORDER BY activity_uid, org_unit_id, role_key
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAssignmentAccessProjection(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AssignmentAccess> findByTargetActorId(UUID targetActorId) {
        return jdbc.query(
            FIND_BY_TARGET_ACTOR_SQL,
            new MapSqlParameterSource("targetActorId", targetActorId),
            (resultSet, rowNumber) -> new AssignmentAccess(
                resultSet.getObject("target_actor_id", UUID.class),
                resultSet.getString("activity_uid"),
                resultSet.getObject("org_unit_id", UUID.class),
                resultSet.getString("role_key")
            )
        );
    }
}
