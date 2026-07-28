package org.nmcpye.datarun.assignmentshadow;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcActorIdentityLink implements ActorIdentityLinkPort {

    private static final String SELECT_COLUMNS = """
        actor_id,
        baseline_user_uid
        """;

    private static final String INSERT_SQL = """
        INSERT INTO actor_identity_link (
            actor_id,
            baseline_user_uid
        )
        VALUES (
            :actorId,
            :baselineUserUid
        )
        RETURNING
        """ + SELECT_COLUMNS;

    private static final String FIND_BY_ACTOR_ID_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM actor_identity_link
        WHERE actor_id = :actorId
        """;

    private static final String FIND_BY_BASELINE_USER_UID_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM actor_identity_link
        WHERE baseline_user_uid = :baselineUserUid
        """;

    private static final RowMapper<ActorIdentityLink> ROW_MAPPER = JdbcActorIdentityLink::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcActorIdentityLink(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public ActorIdentityLink insert(ActorIdentityLink identityLink) {
        return jdbc.queryForObject(
            INSERT_SQL,
            new MapSqlParameterSource()
                .addValue("actorId", identityLink.actorId())
                .addValue("baselineUserUid", identityLink.baselineUserUid()),
            ROW_MAPPER
        );
    }

    @Override
    public Optional<ActorIdentityLink> findByActorId(UUID actorId) {
        return jdbc.query(
            FIND_BY_ACTOR_ID_SQL,
            new MapSqlParameterSource("actorId", actorId),
            ROW_MAPPER
        ).stream().findFirst();
    }

    @Override
    public Optional<ActorIdentityLink> findByBaselineUserUid(String baselineUserUid) {
        return jdbc.query(
            FIND_BY_BASELINE_USER_UID_SQL,
            new MapSqlParameterSource("baselineUserUid", baselineUserUid),
            ROW_MAPPER
        ).stream().findFirst();
    }

    private static ActorIdentityLink mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ActorIdentityLink(
            resultSet.getObject("actor_id", UUID.class),
            resultSet.getString("baseline_user_uid")
        );
    }
}
