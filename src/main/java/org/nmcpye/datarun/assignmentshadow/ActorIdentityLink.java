package org.nmcpye.datarun.assignmentshadow;

import java.util.UUID;

public record ActorIdentityLink(
    UUID actorId,
    String baselineUserUid
) {
}
