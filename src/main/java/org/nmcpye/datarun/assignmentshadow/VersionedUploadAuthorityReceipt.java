package org.nmcpye.datarun.assignmentshadow;

import java.util.Objects;
import java.util.UUID;

public record VersionedUploadAuthorityReceipt(
    String baselineUserUid,
    UUID actorId,
    Acceptance acceptance
) {
    public VersionedUploadAuthorityReceipt {
        Objects.requireNonNull(baselineUserUid);
        Objects.requireNonNull(actorId);
        Objects.requireNonNull(acceptance);
    }

    public sealed interface Acceptance
        permits AssignmentAcceptance, AdministratorAcceptance {
    }

    public record AssignmentAcceptance(UUID grantEventId)
        implements Acceptance {
        public AssignmentAcceptance {
            Objects.requireNonNull(grantEventId);
        }
    }

    public record AdministratorAcceptance() implements Acceptance {
    }

    public static VersionedUploadAuthorityReceipt assignment(
        String baselineUserUid,
        UUID actorId,
        UUID grantEventId
    ) {
        return new VersionedUploadAuthorityReceipt(
            baselineUserUid,
            actorId,
            new AssignmentAcceptance(grantEventId)
        );
    }

    public static VersionedUploadAuthorityReceipt administrator(
        String baselineUserUid
    ) {
        return new VersionedUploadAuthorityReceipt(
            baselineUserUid,
            TransitionIdentityResolver.actorIdFor(baselineUserUid),
            new AdministratorAcceptance()
        );
    }
}
