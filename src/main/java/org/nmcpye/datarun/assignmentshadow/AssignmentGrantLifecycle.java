package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.assignmentshadow.AssignmentAuthoritySnapshot.CaptureIntent;
import org.nmcpye.datarun.assignmentshadow.AssignmentAuthoritySnapshot.IntentKey;
import org.nmcpye.datarun.eventjournal.AppendJournalEvent;
import org.nmcpye.datarun.eventjournal.EventContract;
import org.nmcpye.datarun.eventjournal.EventJournalPort;
import org.nmcpye.datarun.transition.TransitionTimestamp;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class AssignmentGrantLifecycle {

    private static final String EVENT_TYPE = EventContract.ASSIGNMENT_CHANGED;
    private static final String SUBJECT_TYPE = EventContract.ASSIGNMENT;

    private final ActorIdentityLinkPort actors;
    private final TransitionIdentityResolver transitionIdentities;
    private final AssignmentIdentityLinkPort identities;
    private final AssignmentRoleDefinitionPort roles;
    private final AssignmentGrantProjectionPort grants;
    private final EventJournalPort journal;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AssignmentGrantLifecycle(
        ActorIdentityLinkPort actors,
        TransitionIdentityResolver transitionIdentities,
        AssignmentIdentityLinkPort identities,
        AssignmentRoleDefinitionPort roles,
        AssignmentGrantProjectionPort grants,
        EventJournalPort journal,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.actors = actors;
        this.transitionIdentities = transitionIdentities;
        this.identities = identities;
        this.roles = roles;
        this.grants = grants;
        this.journal = journal;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void reconcile(
        Map<IntentKey, CaptureIntent> before,
        Map<IntentKey, CaptureIntent> after,
        String commandActorUserUid
    ) {
        List<IntentKey> keys = new ArrayList<>();
        keys.addAll(before.keySet());
        after.keySet().stream().filter(key -> !before.containsKey(key)).forEach(keys::add);
        keys.sort(Comparator.comparing(IntentKey::assignmentUid).thenComparing(IntentKey::userUid));

        String commandActorId = null;
        for (IntentKey key : keys) {
            CaptureIntent oldIntent = before.get(key);
            CaptureIntent newIntent = after.get(key);
            if (Objects.equals(oldIntent, newIntent)) {
                continue;
            }
            if (commandActorId == null) {
                commandActorId = transitionIdentities
                    .requireOrCreateActor(commandActorUserUid)
                    .actorId()
                    .toString();
            }
            if (oldIntent != null) {
                end(key, oldIntent, commandActorId);
            }
            if (newIntent != null) {
                start(key, newIntent, commandActorId);
            }
        }
    }

    private void start(IntentKey key, CaptureIntent intent, String commandActorId) {
        ActorIdentityLink targetActor =
            transitionIdentities.requireOrCreateActor(key.userUid());
        OrgUnitIdentityLink orgUnit =
            transitionIdentities.requireOrCreateOrgUnit(intent.orgUnitUid());
        AssignmentRoleDefinition role = roles.resolveOrInsert(
            intent.activityUid(),
            intent.formUids()
        );
        List<AssignmentIdentityLink> generations = identities.findGenerations(
            key.assignmentUid(),
            targetActor.actorId()
        );
        int generation = generations.stream()
            .mapToInt(AssignmentIdentityLink::generation)
            .max()
            .orElse(-1) + 1;
        UUID assignmentId = AssignmentShadowIdentities.assignmentId(
            key.assignmentUid(),
            key.userUid(),
            generation
        );
        identities.insert(new AssignmentIdentityLink(
            assignmentId,
            key.assignmentUid(),
            targetActor.actorId(),
            generation
        ));

        UUID eventId = UUID.randomUUID();
        Instant recordedAt = TransitionTimestamp.toDatabasePrecision(
            clock.instant()
        );
        journal.append(new AppendJournalEvent(
            eventId,
            EVENT_TYPE,
            EventContract.ASSIGNMENT_CREATED,
            intent.activityUid(),
            SUBJECT_TYPE,
            assignmentId,
            commandActorId,
            recordedAt,
            assignmentCreatedPayload(
                targetActor.actorId(),
                role.roleKey(),
                orgUnit.orgUnitId(),
                intent.activityUid(),
                recordedAt
            )
        ));
        grants.insert(new AssignmentGrantProjection(
            assignmentId,
            eventId,
            role.roleKey(),
            orgUnit.orgUnitId(),
            AssignmentLifecycleState.ACTIVE
        ));
    }

    private void end(IntentKey key, CaptureIntent intent, String commandActorId) {
        ActorIdentityLink targetActor = actors.findByBaselineUserUid(key.userUid())
            .orElseThrow(() -> conflict("Missing target actor alias for " + key));
        AssignmentIdentityLink identity = identities.findGenerations(
                key.assignmentUid(),
                targetActor.actorId()
            ).stream()
            .max(Comparator.comparingInt(AssignmentIdentityLink::generation))
            .orElseThrow(() -> conflict("Missing assignment generation for " + key));
        AssignmentGrantProjection current = grants.findByAssignmentId(identity.assignmentId())
            .filter(projection -> projection.lifecycleState() == AssignmentLifecycleState.ACTIVE)
            .orElseThrow(() -> conflict("Missing active assignment grant for " + key));

        UUID eventId = UUID.randomUUID();
        Instant recordedAt = TransitionTimestamp.toDatabasePrecision(
            clock.instant()
        );
        journal.append(new AppendJournalEvent(
            eventId,
            EVENT_TYPE,
            EventContract.ASSIGNMENT_ENDED,
            intent.activityUid(),
            SUBJECT_TYPE,
            identity.assignmentId(),
            commandActorId,
            recordedAt,
            objectMapper.createObjectNode().putNull("reason")
        ));
        grants.update(current.sourceEventId(), new AssignmentGrantProjection(
            current.assignmentId(),
            eventId,
            current.roleKey(),
            current.orgUnitId(),
            AssignmentLifecycleState.ENDED
        ));
    }

    private AssignmentAuthorityConflictException conflict(String message) {
        return new AssignmentAuthorityConflictException(message);
    }

    private ObjectNode assignmentCreatedPayload(
        UUID targetActorId,
        String roleKey,
        UUID orgUnitId,
        String activityUid,
        Instant validFrom
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putObject("target_actor")
            .put("type", "actor")
            .put("id", targetActorId.toString());
        payload.put("role", roleKey);
        ObjectNode scope = payload.putObject("scope");
        scope.put("geographic", orgUnitId.toString());
        scope.putNull("subject_list");
        scope.putArray("activity").add(activityUid);
        payload.put("valid_from", validFrom.toString());
        payload.putNull("valid_to");
        return payload;
    }
}
