package org.nmcpye.datarun.captureshadow;

import java.util.Optional;
import java.util.UUID;

public interface CaptureCurrentProjectionPort {

    void insertBootstrapPointer(UUID captureId, UUID sourceEventId);

    Optional<UUID> findSourceEventId(UUID captureId);
}
