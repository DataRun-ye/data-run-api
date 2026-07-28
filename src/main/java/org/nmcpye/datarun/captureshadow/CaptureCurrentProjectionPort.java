package org.nmcpye.datarun.captureshadow;

import java.util.Optional;
import java.util.UUID;

public interface CaptureCurrentProjectionPort {

    void insertInitialPointer(UUID captureId, UUID sourceEventId);

    default void insertBootstrapPointer(UUID captureId, UUID sourceEventId) {
        insertInitialPointer(captureId, sourceEventId);
    }

    Optional<UUID> findSourceEventId(UUID captureId);

    Optional<UUID> findSourceEventIdForUpdate(UUID captureId);

    void compareAndSwap(
        UUID captureId,
        UUID expectedSourceEventId,
        UUID newSourceEventId
    );
}
