package org.nmcpye.datarun.assignmentshadow.replay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.assignmentshadow.AssignmentAuthorityCommandService;
import org.nmcpye.datarun.assignmentshadow.AssignmentLifecycleState;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowCheckpoint;
import org.nmcpye.datarun.eventjournal.EventContract;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AssignmentProjectionReplay {

    private static final int BATCH_SIZE = 1_000;
    private static final Set<String> BASELINE_FIELDS = Set.of(
        "role",
        "org_unit_id",
        "lifecycle_state"
    );
    private static final Set<String> CREATED_FIELDS = Set.of(
        "target_actor",
        "role",
        "scope",
        "valid_from",
        "valid_to"
    );
    private static final Set<String> TARGET_ACTOR_FIELDS = Set.of("type", "id");
    private static final Set<String> SCOPE_FIELDS = Set.of(
        "geographic",
        "subject_list",
        "activity"
    );
    private static final Set<String> ENDED_FIELDS = Set.of("reason");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AssignmentShadowCheckpoint checkpoint;

    public AssignmentProjectionReplay(
        JdbcTemplate jdbc,
        ObjectMapper objectMapper,
        AssignmentShadowCheckpoint checkpoint
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.checkpoint = checkpoint;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AssignmentProjectionReplayReport replay(
        AssignmentProjectionReplayMode mode
    ) {
        Objects.requireNonNull(mode);
        jdbc.execute(
            "SELECT pg_advisory_xact_lock("
                + AssignmentAuthorityCommandService.ADVISORY_LOCK_KEY
                + ")"
        );
        checkpoint.requireCompleted();

        ReplayInputs inputs = loadInputs();
        ReplayState replay = derive(inputs);
        Comparison before = compare(replay.expected());
        if (!before.exact() && mode == AssignmentProjectionReplayMode.VALIDATE) {
            throw conflict(before.describe());
        }

        long rebuilt = 0;
        if (!before.exact()) {
            rebuilt = rebuild(replay.expected());
            Comparison after = compare(replay.expected());
            if (!after.exact()) {
                throw conflict("Projection repair did not converge: " + after.describe());
            }
        }

        return new AssignmentProjectionReplayReport(
            replay.eventCount(),
            replay.expected().size(),
            before.missing(),
            before.unexpected(),
            before.differing(),
            rebuilt
        );
    }

    private ReplayInputs loadInputs() {
        Map<UUID, AssignmentIdentity> identities = jdbc.query(
            """
                SELECT assignment_id, target_actor_id
                FROM assignment_identity_link
                ORDER BY assignment_id
                """,
            resultSet -> {
                Map<UUID, AssignmentIdentity> values = new LinkedHashMap<>();
                while (resultSet.next()) {
                    UUID assignmentId = resultSet.getObject(
                        "assignment_id",
                        UUID.class
                    );
                    values.put(
                        assignmentId,
                        new AssignmentIdentity(
                            assignmentId,
                            resultSet.getObject("target_actor_id", UUID.class)
                        )
                    );
                }
                return values;
            }
        );
        Map<String, RoleDefinition> roles = jdbc.query(
            """
                SELECT role_key, activity_uid
                FROM assignment_role_definition
                ORDER BY role_key
                """,
            resultSet -> {
                Map<String, RoleDefinition> values = new LinkedHashMap<>();
                while (resultSet.next()) {
                    String roleKey = resultSet.getString("role_key");
                    values.put(
                        roleKey,
                        new RoleDefinition(
                            roleKey,
                            resultSet.getString("activity_uid")
                        )
                    );
                }
                return values;
            }
        );
        Set<UUID> orgUnits = new HashSet<>(jdbc.queryForList(
            "SELECT org_unit_id FROM org_unit_identity_link",
            UUID.class
        ));
        List<AssignmentFact> facts = jdbc.query(
            """
                SELECT event_id, shape_ref, activity_ref, subject_id,
                       recorded_at, payload::text AS payload
                FROM event_journal
                WHERE event_type = 'assignment_changed'
                  AND subject_type = 'assignment'
                ORDER BY journal_position
                """,
            (resultSet, rowNumber) -> new AssignmentFact(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("shape_ref"),
                resultSet.getString("activity_ref"),
                resultSet.getObject("subject_id", UUID.class),
                resultSet.getTimestamp("recorded_at").toInstant(),
                readJson(resultSet.getString("payload"))
            )
        );
        return new ReplayInputs(identities, roles, orgUnits, facts);
    }

    private ReplayState derive(ReplayInputs inputs) {
        Map<UUID, ExpectedProjection> expected = new LinkedHashMap<>();
        for (AssignmentFact fact : inputs.facts()) {
            AssignmentIdentity identity = inputs.identities().get(
                fact.assignmentId()
            );
            if (identity == null) {
                throw conflict(
                    "Assignment fact has no immutable identity link: "
                        + fact.eventId()
                );
            }
            ExpectedProjection current = expected.get(fact.assignmentId());
            ExpectedProjection next = switch (fact.shapeRef()) {
                case EventContract.BASELINE_ASSIGNMENT_OBSERVED ->
                    observed(fact, current, inputs);
                case EventContract.ASSIGNMENT_CREATED ->
                    created(fact, current, identity, inputs);
                case EventContract.ASSIGNMENT_ENDED -> ended(fact, current);
                default -> throw conflict(
                    "Unsupported assignment fact shape: " + fact.shapeRef()
                );
            };
            expected.put(fact.assignmentId(), next);
        }
        if (!expected.keySet().equals(inputs.identities().keySet())) {
            Set<UUID> missing = new HashSet<>(inputs.identities().keySet());
            missing.removeAll(expected.keySet());
            throw conflict(
                "Assignment identities without lifecycle facts: "
                    + sample(missing)
            );
        }
        return new ReplayState(inputs.facts().size(), expected);
    }

    private ExpectedProjection observed(
        AssignmentFact fact,
        ExpectedProjection current,
        ReplayInputs inputs
    ) {
        requireNoCurrent(fact, current);
        requireFields(fact.payload(), BASELINE_FIELDS, fact);
        String roleKey = requiredText(fact.payload(), "role", fact);
        UUID orgUnitId = requiredUuid(fact.payload(), "org_unit_id", fact);
        AssignmentLifecycleState state = lifecycle(
            requiredText(fact.payload(), "lifecycle_state", fact),
            fact
        );
        requireReferences(fact, roleKey, orgUnitId, inputs);
        return new ExpectedProjection(
            fact.assignmentId(),
            fact.eventId(),
            roleKey,
            orgUnitId,
            state,
            fact.activityRef()
        );
    }

    private ExpectedProjection created(
        AssignmentFact fact,
        ExpectedProjection current,
        AssignmentIdentity identity,
        ReplayInputs inputs
    ) {
        requireNoCurrent(fact, current);
        JsonNode payload = fact.payload();
        requireFields(payload, CREATED_FIELDS, fact);

        JsonNode targetActor = requiredObject(payload, "target_actor", fact);
        requireFields(targetActor, TARGET_ACTOR_FIELDS, fact);
        if (!"actor".equals(requiredText(targetActor, "type", fact))) {
            throw conflict("Assignment target actor type is invalid: " + fact.eventId());
        }
        UUID targetActorId = requiredUuid(targetActor, "id", fact);
        if (!targetActorId.equals(identity.targetActorId())) {
            throw conflict("Assignment target actor conflicts with identity: " + fact.eventId());
        }

        String roleKey = requiredText(payload, "role", fact);
        JsonNode scope = requiredObject(payload, "scope", fact);
        requireFields(scope, SCOPE_FIELDS, fact);
        UUID orgUnitId = requiredUuid(scope, "geographic", fact);
        if (!scope.path("subject_list").isNull()) {
            throw conflict("Initial assignment scope requires subject_list=null: " + fact.eventId());
        }
        JsonNode activities = scope.path("activity");
        if (!activities.isArray()
            || activities.size() != 1
            || !activities.get(0).isTextual()
            || !Objects.equals(activities.get(0).textValue(), fact.activityRef())) {
            throw conflict("Assignment activity scope is not exact: " + fact.eventId());
        }
        Instant validFrom = requiredInstant(payload, "valid_from", fact);
        if (!validFrom.equals(fact.recordedAt())) {
            throw conflict("Assignment valid_from differs from recorded_at: " + fact.eventId());
        }
        if (!payload.path("valid_to").isNull()) {
            throw conflict("Initial assignment lifecycle requires valid_to=null: " + fact.eventId());
        }
        requireReferences(fact, roleKey, orgUnitId, inputs);
        return new ExpectedProjection(
            fact.assignmentId(),
            fact.eventId(),
            roleKey,
            orgUnitId,
            AssignmentLifecycleState.ACTIVE,
            fact.activityRef()
        );
    }

    private ExpectedProjection ended(
        AssignmentFact fact,
        ExpectedProjection current
    ) {
        if (current == null
            || current.lifecycleState() != AssignmentLifecycleState.ACTIVE) {
            throw conflict("Assignment ended without an active predecessor: " + fact.eventId());
        }
        requireFields(fact.payload(), ENDED_FIELDS, fact);
        JsonNode reason = fact.payload().get("reason");
        if (reason == null || (!reason.isNull() && !reason.isTextual())) {
            throw conflict("Assignment end reason is invalid: " + fact.eventId());
        }
        if (!Objects.equals(fact.activityRef(), current.activityRef())) {
            throw conflict("Assignment end activity differs from its predecessor: " + fact.eventId());
        }
        return new ExpectedProjection(
            current.assignmentId(),
            fact.eventId(),
            current.roleKey(),
            current.orgUnitId(),
            AssignmentLifecycleState.ENDED,
            current.activityRef()
        );
    }

    private void requireNoCurrent(
        AssignmentFact fact,
        ExpectedProjection current
    ) {
        if (current != null) {
            throw conflict("Assignment has more than one start fact: " + fact.assignmentId());
        }
    }

    private void requireReferences(
        AssignmentFact fact,
        String roleKey,
        UUID orgUnitId,
        ReplayInputs inputs
    ) {
        RoleDefinition role = inputs.roles().get(roleKey);
        if (role == null) {
            throw conflict("Assignment fact references an unknown role: " + fact.eventId());
        }
        if (!Objects.equals(role.activityUid(), fact.activityRef())) {
            throw conflict("Assignment role activity differs from event activity: " + fact.eventId());
        }
        if (!inputs.orgUnitIds().contains(orgUnitId)) {
            throw conflict("Assignment fact references an unknown org unit: " + fact.eventId());
        }
    }

    private Comparison compare(Map<UUID, ExpectedProjection> expected) {
        Map<UUID, ActualProjection> actual = jdbc.query(
            """
                SELECT assignment_id, source_event_id, role_key, org_unit_id,
                       lifecycle_state
                FROM assignment_grant_projection
                ORDER BY assignment_id
                """,
            resultSet -> {
                Map<UUID, ActualProjection> values = new LinkedHashMap<>();
                while (resultSet.next()) {
                    UUID assignmentId = resultSet.getObject("assignment_id", UUID.class);
                    values.put(
                        assignmentId,
                        new ActualProjection(
                            assignmentId,
                            resultSet.getObject("source_event_id", UUID.class),
                            resultSet.getString("role_key"),
                            resultSet.getObject("org_unit_id", UUID.class),
                            AssignmentLifecycleState.valueOf(
                                resultSet.getString("lifecycle_state")
                            )
                        )
                    );
                }
                return values;
            }
        );
        long missing = expected.keySet().stream()
            .filter(id -> !actual.containsKey(id))
            .count();
        long unexpected = actual.keySet().stream()
            .filter(id -> !expected.containsKey(id))
            .count();
        List<UUID> differing = new ArrayList<>();
        expected.forEach((id, value) -> {
            ActualProjection actualValue = actual.get(id);
            if (actualValue != null && !value.matches(actualValue)) {
                differing.add(id);
            }
        });
        return new Comparison(missing, unexpected, differing.size(), differing);
    }

    private long rebuild(Map<UUID, ExpectedProjection> expected) {
        jdbc.update("DELETE FROM assignment_grant_projection");
        List<ExpectedProjection> rows = List.copyOf(expected.values());
        jdbc.batchUpdate(
            """
                INSERT INTO assignment_grant_projection (
                    assignment_id,
                    source_event_id,
                    role_key,
                    org_unit_id,
                    lifecycle_state
                ) VALUES (?, ?, ?, ?, ?)
                """,
            rows,
            BATCH_SIZE,
            (statement, row) -> {
                statement.setObject(1, row.assignmentId());
                statement.setObject(2, row.sourceEventId());
                statement.setString(3, row.roleKey());
                statement.setObject(4, row.orgUnitId());
                statement.setString(5, row.lifecycleState().name());
            }
        );
        return rows.size();
    }

    private void requireFields(
        JsonNode value,
        Set<String> expected,
        AssignmentFact fact
    ) {
        if (!value.isObject()) {
            throw conflict("Assignment payload is not an object: " + fact.eventId());
        }
        Set<String> actual = new HashSet<>();
        value.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw conflict(
                "Assignment payload fields differ for " + fact.eventId()
                    + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private JsonNode requiredObject(
        JsonNode parent,
        String field,
        AssignmentFact fact
    ) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()) {
            throw conflict("Assignment field " + field + " is not an object: " + fact.eventId());
        }
        return value;
    }

    private String requiredText(
        JsonNode parent,
        String field,
        AssignmentFact fact
    ) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw conflict("Assignment field " + field + " is invalid: " + fact.eventId());
        }
        return value.textValue();
    }

    private UUID requiredUuid(
        JsonNode parent,
        String field,
        AssignmentFact fact
    ) {
        try {
            return UUID.fromString(requiredText(parent, field, fact));
        } catch (IllegalArgumentException exception) {
            throw conflict("Assignment field " + field + " is not a UUID: " + fact.eventId());
        }
    }

    private Instant requiredInstant(
        JsonNode parent,
        String field,
        AssignmentFact fact
    ) {
        try {
            return Instant.parse(requiredText(parent, field, fact));
        } catch (DateTimeParseException exception) {
            throw conflict("Assignment field " + field + " is not an instant: " + fact.eventId());
        }
    }

    private AssignmentLifecycleState lifecycle(
        String value,
        AssignmentFact fact
    ) {
        try {
            return AssignmentLifecycleState.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw conflict("Assignment lifecycle is invalid: " + fact.eventId());
        }
    }

    private JsonNode readJson(String value) throws java.sql.SQLException {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new java.sql.SQLException("Stored assignment fact is invalid JSON", exception);
        }
    }

    private AssignmentProjectionReplayConflictException conflict(String message) {
        return new AssignmentProjectionReplayConflictException(message);
    }

    private static String sample(Set<UUID> values) {
        return values.stream().limit(10).toList().toString();
    }

    private record ReplayInputs(
        Map<UUID, AssignmentIdentity> identities,
        Map<String, RoleDefinition> roles,
        Set<UUID> orgUnitIds,
        List<AssignmentFact> facts
    ) {
    }

    private record AssignmentIdentity(UUID assignmentId, UUID targetActorId) {
    }

    private record RoleDefinition(String roleKey, String activityUid) {
    }

    private record AssignmentFact(
        UUID eventId,
        String shapeRef,
        String activityRef,
        UUID assignmentId,
        Instant recordedAt,
        JsonNode payload
    ) {
    }

    private record ExpectedProjection(
        UUID assignmentId,
        UUID sourceEventId,
        String roleKey,
        UUID orgUnitId,
        AssignmentLifecycleState lifecycleState,
        String activityRef
    ) {
        private boolean matches(ActualProjection actual) {
            return assignmentId.equals(actual.assignmentId())
                && sourceEventId.equals(actual.sourceEventId())
                && roleKey.equals(actual.roleKey())
                && orgUnitId.equals(actual.orgUnitId())
                && lifecycleState == actual.lifecycleState();
        }
    }

    private record ActualProjection(
        UUID assignmentId,
        UUID sourceEventId,
        String roleKey,
        UUID orgUnitId,
        AssignmentLifecycleState lifecycleState
    ) {
    }

    private record ReplayState(
        long eventCount,
        Map<UUID, ExpectedProjection> expected
    ) {
    }

    private record Comparison(
        long missing,
        long unexpected,
        long differing,
        List<UUID> differingSamples
    ) {
        private boolean exact() {
            return missing == 0 && unexpected == 0 && differing == 0;
        }

        private String describe() {
            return "Assignment projection differs from replay"
                + " missing=" + missing
                + " unexpected=" + unexpected
                + " differing=" + differing
                + " samples=" + differingSamples.stream().limit(10).toList();
        }
    }
}
