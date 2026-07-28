package org.nmcpye.datarun.assignmentshadow;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransitionIdentityResolver {

    private final ActorIdentityLinkPort actors;
    private final OrgUnitIdentityLinkPort orgUnits;

    public TransitionIdentityResolver(
        ActorIdentityLinkPort actors,
        OrgUnitIdentityLinkPort orgUnits
    ) {
        this.actors = actors;
        this.orgUnits = orgUnits;
    }

    public static UUID actorIdFor(String userUid) {
        return AssignmentShadowIdentities.actorId(userUid);
    }

    public static UUID orgUnitIdFor(String orgUnitUid) {
        return AssignmentShadowIdentities.orgUnitId(orgUnitUid);
    }

    public ActorIdentityLink requireOrCreateActor(String userUid) {
        UUID actorId = actorIdFor(userUid);
        return actors.findByBaselineUserUid(userUid)
            .map(existing -> {
                if (!existing.actorId().equals(actorId)) {
                    throw conflict("Conflicting actor alias for user " + userUid);
                }
                return existing;
            })
            .orElseGet(() -> actors.insert(new ActorIdentityLink(actorId, userUid)));
    }

    public OrgUnitIdentityLink requireOrCreateOrgUnit(String orgUnitUid) {
        UUID orgUnitId = orgUnitIdFor(orgUnitUid);
        return orgUnits.findByBaselineOrgUnitUid(orgUnitUid)
            .map(existing -> {
                if (!existing.orgUnitId().equals(orgUnitId)) {
                    throw conflict(
                        "Conflicting organization-unit alias for " + orgUnitUid
                    );
                }
                return existing;
            })
            .orElseGet(() ->
                orgUnits.insert(new OrgUnitIdentityLink(orgUnitId, orgUnitUid))
            );
    }

    private AssignmentAuthorityConflictException conflict(String message) {
        return new AssignmentAuthorityConflictException(message);
    }
}
