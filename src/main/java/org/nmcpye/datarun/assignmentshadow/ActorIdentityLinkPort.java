package org.nmcpye.datarun.assignmentshadow;

import java.util.Optional;
import java.util.UUID;

public interface ActorIdentityLinkPort {

    ActorIdentityLink insert(ActorIdentityLink identityLink);

    Optional<ActorIdentityLink> findByActorId(UUID actorId);

    Optional<ActorIdentityLink> findByBaselineUserUid(String baselineUserUid);
}
