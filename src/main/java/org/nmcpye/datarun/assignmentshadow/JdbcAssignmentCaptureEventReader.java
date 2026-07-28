package org.nmcpye.datarun.assignmentshadow;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@Transactional(
    propagation = Propagation.REQUIRES_NEW,
    readOnly = true
)
public class JdbcAssignmentCaptureEventReader
    implements AssignmentCaptureEventReadPort {

    private static final String SELECT_GRANTS_SQL = """
        SELECT
            identity.baseline_assignment_uid,
            identity.assignment_id,
            grant_projection.source_event_id,
            identity.target_actor_id,
            identity.generation,
            role.activity_uid,
            grant_projection.org_unit_id,
            org_unit.baseline_org_unit_uid,
            role.form_uids::text AS form_uids,
            grant_projection.lifecycle_state
        FROM assignment_identity_link identity
        JOIN assignment_grant_projection grant_projection
          ON grant_projection.assignment_id = identity.assignment_id
        JOIN actor_identity_link actor
          ON actor.actor_id = identity.target_actor_id
        JOIN assignment_role_definition role
          ON role.role_key = grant_projection.role_key
        JOIN org_unit_identity_link org_unit
          ON org_unit.org_unit_id = grant_projection.org_unit_id
        WHERE identity.target_actor_id = :targetActorId
          AND actor.baseline_user_uid = :baselineUserUid
        """;

    private static final String ASSIGNMENT_FILTER_SQL =
        " AND identity.baseline_assignment_uid IN (:assignmentUids)";

    private static final String ORDER_SQL =
        " ORDER BY identity.baseline_assignment_uid, identity.generation";

    private final NamedParameterJdbcTemplate jdbc;
    private final ActorIdentityLinkPort actorIdentityLinks;
    private final AssignmentShadowCheckpoint checkpoint;
    private final CanonicalCaptureFormResolver captureForms;

    public JdbcAssignmentCaptureEventReader(
        NamedParameterJdbcTemplate jdbc,
        ActorIdentityLinkPort actorIdentityLinks,
        AssignmentShadowCheckpoint checkpoint,
        CanonicalCaptureFormResolver captureForms
    ) {
        this.jdbc = jdbc;
        this.actorIdentityLinks = actorIdentityLinks;
        this.checkpoint = checkpoint;
        this.captureForms = captureForms;
    }

    @Override
    public AssignmentCaptureEventSnapshot readAllForActor(
        String baselineUserUid
    ) {
        return read(baselineUserUid, null);
    }

    @Override
    public AssignmentCaptureEventSnapshot readAssignments(
        String baselineUserUid,
        Collection<String> assignmentUids
    ) {
        return read(baselineUserUid, Set.copyOf(assignmentUids));
    }

    private AssignmentCaptureEventSnapshot read(
        String baselineUserUid,
        Set<String> assignmentUids
    ) {
        if (!checkpoint.existsAndIsExact()) {
            return AssignmentCaptureEventSnapshot.unavailable();
        }

        ActorIdentityLink actor = actorIdentityLinks
            .findByBaselineUserUid(baselineUserUid)
            .orElse(null);
        if (actor == null) {
            return AssignmentCaptureEventSnapshot.actorAliasAbsent();
        }
        if (assignmentUids != null && assignmentUids.isEmpty()) {
            return AssignmentCaptureEventSnapshot.available(
                actor.actorId(),
                List.of()
            );
        }

        String sql = SELECT_GRANTS_SQL
            + (assignmentUids == null ? "" : ASSIGNMENT_FILTER_SQL)
            + ORDER_SQL;
        MapSqlParameterSource parameters = new MapSqlParameterSource(
            "targetActorId",
            actor.actorId()
        ).addValue("baselineUserUid", baselineUserUid);
        if (assignmentUids != null) {
            parameters.addValue("assignmentUids", assignmentUids);
        }

        List<AssignmentCaptureEventGrant> grants = jdbc.query(
            sql,
            parameters,
            (resultSet, rowNumber) -> new AssignmentCaptureEventGrant(
                resultSet.getString("baseline_assignment_uid"),
                resultSet.getObject("assignment_id", UUID.class),
                resultSet.getObject("source_event_id", UUID.class),
                resultSet.getObject("target_actor_id", UUID.class),
                resultSet.getInt("generation"),
                resultSet.getString("activity_uid"),
                resultSet.getObject("org_unit_id", UUID.class),
                resultSet.getString("baseline_org_unit_uid"),
                captureForms.readCanonicalFormUidList(
                    resultSet.getString("form_uids"),
                    "assignment_role_definition.form_uids"
                ),
                AssignmentLifecycleState.valueOf(
                    resultSet.getString("lifecycle_state")
                )
            )
        );
        return AssignmentCaptureEventSnapshot.available(
            actor.actorId(),
            grants
        );
    }
}
