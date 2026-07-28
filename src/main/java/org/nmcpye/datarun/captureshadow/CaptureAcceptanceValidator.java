package org.nmcpye.datarun.captureshadow;

import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.assignmentshadow.TransitionIdentityResolver;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class CaptureAcceptanceValidator {

    private static final Set<String> ASSIGNMENT_EVENT_SHAPES = Set.of(
        "baseline_assignment_observed/v1",
        "assignment_created/v1",
        "assignment_ended/v1"
    );

    private final NamedParameterJdbcTemplate jdbc;

    public CaptureAcceptanceValidator(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void validate(
        JsonNode acceptance,
        UUID actorId,
        String baselineAssignmentUid
    ) {
        validateAll(List.of(new AcceptanceCheck(
            acceptance,
            actorId,
            baselineAssignmentUid,
            false
        )));
    }

    public void validateCurrentGrant(
        JsonNode acceptance,
        UUID actorId,
        String baselineAssignmentUid
    ) {
        validateAll(List.of(new AcceptanceCheck(
            acceptance,
            actorId,
            baselineAssignmentUid,
            true
        )));
    }

    public void validateAll(Collection<AcceptanceCheck> checks) {
        if (checks == null || checks.isEmpty()) {
            return;
        }
        List<ParsedCheck> parsed = checks.stream()
            .map(this::parse)
            .toList();
        Map<UUID, String> actors = actors(parsed);
        Map<UUID, GrantEvent> grantEvents = grantEvents(parsed);
        Map<UUID, AssignmentIdentity> assignments =
            assignments(grantEvents.values());
        Map<UUID, UUID> currentGrantEvents =
            currentGrantEvents(parsed, assignments);

        for (ParsedCheck check : parsed) {
            String baselineUserUid = actors.get(check.actorId());
            if (baselineUserUid == null) {
                throw conflict(
                    "Capture actor alias does not exist: " + check.actorId()
                );
            }
            if (!TransitionIdentityResolver.actorIdFor(baselineUserUid)
                .equals(check.actorId())) {
                throw conflict("Capture actor alias is not deterministic");
            }
            if (check.grantEventId() == null) {
                continue;
            }

            GrantEvent grant = grantEvents.get(check.grantEventId());
            if (grant == null) {
                throw conflict(
                    "Assignment grant event does not exist: "
                        + check.grantEventId()
                );
            }
            if (!"assignment_changed".equals(grant.eventType())
                || !ASSIGNMENT_EVENT_SHAPES.contains(grant.shapeRef())
                || !"assignment".equals(grant.subjectType())) {
                throw conflict(
                    "Capture acceptance does not reference an assignment grant event"
                );
            }
            AssignmentIdentity identity = assignments.get(grant.subjectId());
            if (identity == null
                || !identity.baselineAssignmentUid().equals(
                    check.baselineAssignmentUid()
                )
                || !identity.targetActorId().equals(check.actorId())) {
                throw conflict(
                    "Assignment grant does not match capture actor and assignment"
                );
            }
            if (check.requireCurrentProjection()
                && !Objects.equals(
                    currentGrantEvents.get(identity.assignmentId()),
                    check.grantEventId()
                )) {
                throw conflict(
                    "Assignment grant event is no longer the accepted projection"
                );
            }
        }
    }

    private ParsedCheck parse(AcceptanceCheck check) {
        Objects.requireNonNull(check);
        Objects.requireNonNull(check.actorId());
        Objects.requireNonNull(check.baselineAssignmentUid());
        JsonNode acceptance = check.acceptance();
        if (acceptance == null || !acceptance.isObject()) {
            throw conflict("Capture acceptance must be an object");
        }
        String kind = acceptance.path("kind").textValue();
        if ("administrator".equals(kind)) {
            if (acceptance.size() != 1) {
                throw conflict(
                    "Administrator capture acceptance contains invented fields"
                );
            }
            return new ParsedCheck(
                check.actorId(),
                check.baselineAssignmentUid(),
                null,
                check.requireCurrentProjection()
            );
        }
        if (!"assignment".equals(kind)
            || acceptance.size() != 2
            || !acceptance.path("grantEventId").isTextual()) {
            throw conflict("Assignment capture acceptance is malformed");
        }
        return new ParsedCheck(
            check.actorId(),
            check.baselineAssignmentUid(),
            parseUuid(
                acceptance.path("grantEventId").textValue(),
                "assignment grant event"
            ),
            check.requireCurrentProjection()
        );
    }

    private Map<UUID, String> actors(List<ParsedCheck> checks) {
        Set<UUID> actorIds = checks.stream()
            .map(ParsedCheck::actorId)
            .collect(java.util.stream.Collectors.toSet());
        return uniqueMap(
            jdbc.query(
                """
                    SELECT actor_id, baseline_user_uid
                    FROM actor_identity_link
                    WHERE actor_id IN (:actorIds)
                    """,
                new MapSqlParameterSource("actorIds", actorIds),
                (resultSet, rowNumber) -> Map.entry(
                    resultSet.getObject("actor_id", UUID.class),
                    resultSet.getString("baseline_user_uid")
                )
            ),
            "actor aliases"
        );
    }

    private Map<UUID, GrantEvent> grantEvents(List<ParsedCheck> checks) {
        Set<UUID> eventIds = checks.stream()
            .map(ParsedCheck::grantEventId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return uniqueMap(
            jdbc.query(
                """
                    SELECT event_id, event_type, shape_ref,
                           subject_type, subject_id
                    FROM event_journal
                    WHERE event_id IN (:eventIds)
                    """,
                new MapSqlParameterSource("eventIds", eventIds),
                (resultSet, rowNumber) -> Map.entry(
                    resultSet.getObject("event_id", UUID.class),
                    new GrantEvent(
                        resultSet.getString("event_type"),
                        resultSet.getString("shape_ref"),
                        resultSet.getString("subject_type"),
                        resultSet.getObject("subject_id", UUID.class)
                    )
                )
            ),
            "assignment grant events"
        );
    }

    private Map<UUID, AssignmentIdentity> assignments(
        Collection<GrantEvent> grants
    ) {
        Set<UUID> assignmentIds = grants.stream()
            .map(GrantEvent::subjectId)
            .collect(java.util.stream.Collectors.toSet());
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return uniqueMap(
            jdbc.query(
                """
                    SELECT assignment_id, baseline_assignment_uid,
                           target_actor_id
                    FROM assignment_identity_link
                    WHERE assignment_id IN (:assignmentIds)
                    """,
                new MapSqlParameterSource(
                    "assignmentIds",
                    assignmentIds
                ),
                (resultSet, rowNumber) -> {
                    UUID assignmentId = resultSet.getObject(
                        "assignment_id",
                        UUID.class
                    );
                    return Map.entry(
                        assignmentId,
                        new AssignmentIdentity(
                            assignmentId,
                            resultSet.getString(
                                "baseline_assignment_uid"
                            ),
                            resultSet.getObject(
                                "target_actor_id",
                                UUID.class
                            )
                        )
                    );
                }
            ),
            "assignment identities"
        );
    }

    private Map<UUID, UUID> currentGrantEvents(
        List<ParsedCheck> checks,
        Map<UUID, AssignmentIdentity> assignments
    ) {
        if (checks.stream().noneMatch(
            ParsedCheck::requireCurrentProjection
        ) || assignments.isEmpty()) {
            return Map.of();
        }
        return uniqueMap(
            jdbc.query(
                """
                    SELECT assignment_id, source_event_id
                    FROM assignment_grant_projection
                    WHERE assignment_id IN (:assignmentIds)
                    """,
                new MapSqlParameterSource(
                    "assignmentIds",
                    assignments.keySet()
                ),
                (resultSet, rowNumber) -> Map.entry(
                    resultSet.getObject("assignment_id", UUID.class),
                    resultSet.getObject("source_event_id", UUID.class)
                )
            ),
            "assignment grant projections"
        );
    }

    private <T> Map<UUID, T> uniqueMap(
        List<Map.Entry<UUID, T>> entries,
        String label
    ) {
        Map<UUID, T> values = new LinkedHashMap<>();
        for (Map.Entry<UUID, T> entry : entries) {
            if (values.put(entry.getKey(), entry.getValue()) != null) {
                throw conflict("Conflicting " + label);
            }
        }
        return Map.copyOf(values);
    }

    private UUID parseUuid(String value, String label) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw conflict("Invalid " + label + " UUID");
        }
    }

    private CaptureShadowConflictException conflict(String message) {
        return new CaptureShadowConflictException(message);
    }

    public record AcceptanceCheck(
        JsonNode acceptance,
        UUID actorId,
        String baselineAssignmentUid,
        boolean requireCurrentProjection
    ) {
    }

    private record ParsedCheck(
        UUID actorId,
        String baselineAssignmentUid,
        UUID grantEventId,
        boolean requireCurrentProjection
    ) {
    }

    private record GrantEvent(
        String eventType,
        String shapeRef,
        String subjectType,
        UUID subjectId
    ) {
    }

    private record AssignmentIdentity(
        UUID assignmentId,
        String baselineAssignmentUid,
        UUID targetActorId
    ) {
    }
}
