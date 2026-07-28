package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.assignmentshadow.AssignmentAuthoritySnapshot.CaptureIntent;
import org.nmcpye.datarun.assignmentshadow.AssignmentAuthoritySnapshot.IntentKey;
import org.nmcpye.datarun.eventjournal.AppendJournalEvent;
import org.nmcpye.datarun.eventjournal.EventJournalPort;
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

    private static final String EVENT_TYPE = "assignment_changed";
    private static final String SUBJECT_TYPE = "assignment";

    private final ActorIdentityLinkPort actors;
    private final OrgUnitIdentityLinkPort orgUnits;
    private final AssignmentIdentityLinkPort identities;
    private final AssignmentRoleDefinitionPort roles;
    private final AssignmentGrantProjectionPort grants;
    private final EventJournalPort journal;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AssignmentGrantLifecycle(
        ActorIdentityLinkPort actors,
        OrgUnitIdentityLinkPort orgUnits,
        AssignmentIdentityLinkPort identities,
        AssignmentRoleDefinitionPort roles,
        AssignmentGrantProjectionPort grants,
        EventJournalPort journal,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.actors = actors;
        this.orgUnits = orgUnits;
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
                commandActorId = ensureActor(commandActorUserUid).actorId().toString();
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
        ActorIdentityLink targetActor = ensureActor(key.userUid());
        OrgUnitIdentityLink orgUnit = ensureOrgUnit(intent.orgUnitUid());
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
        Instant recordedAt = clock.instant();
        journal.append(new AppendJournalEvent(
            eventId,
            EVENT_TYPE,
            "assignment_created/v1",
            intent.activityUid(),
            SUBJECT_TYPE,
            assignmentId,
            commandActorId,
            recordedAt,
            objectMapper.valueToTree(Map.of(
                "role", role.roleKey(),
                "org_unit_id", orgUnit.orgUnitId().toString()
            ))
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
        journal.append(new AppendJournalEvent(
            eventId,
            EVENT_TYPE,
            "assignment_ended/v1",
            intent.activityUid(),
            SUBJECT_TYPE,
            identity.assignmentId(),
            commandActorId,
            clock.instant(),
            objectMapper.createObjectNode()
        ));
        grants.update(current.sourceEventId(), new AssignmentGrantProjection(
            current.assignmentId(),
            eventId,
            current.roleKey(),
            current.orgUnitId(),
            AssignmentLifecycleState.ENDED
        ));
    }

    private ActorIdentityLink ensureActor(String userUid) {
        UUID actorId = AssignmentShadowIdentities.actorId(userUid);
        return actors.findByBaselineUserUid(userUid)
            .map(existing -> {
                if (!existing.actorId().equals(actorId)) {
                    throw conflict("Conflicting actor alias for user " + userUid);
                }
                return existing;
            })
            .orElseGet(() -> actors.insert(new ActorIdentityLink(actorId, userUid)));
    }

    private OrgUnitIdentityLink ensureOrgUnit(String orgUnitUid) {
        UUID orgUnitId = AssignmentShadowIdentities.orgUnitId(orgUnitUid);
        return orgUnits.findByBaselineOrgUnitUid(orgUnitUid)
            .map(existing -> {
                if (!existing.orgUnitId().equals(orgUnitId)) {
                    throw conflict("Conflicting organization-unit alias for " + orgUnitUid);
                }
                return existing;
            })
            .orElseGet(() -> orgUnits.insert(new OrgUnitIdentityLink(orgUnitId, orgUnitUid)));
    }

    private AssignmentAuthorityConflictException conflict(String message) {
        return new AssignmentAuthorityConflictException(message);
    }
}
