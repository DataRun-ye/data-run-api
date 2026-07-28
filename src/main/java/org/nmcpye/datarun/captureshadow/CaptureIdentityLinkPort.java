package org.nmcpye.datarun.captureshadow;

import java.util.Optional;
import java.util.UUID;

public interface CaptureIdentityLinkPort {

    CaptureIdentityResolution resolveOrInsert(CaptureIdentityLink identity);

    CaptureIdentityLink requireExact(CaptureIdentityLink identity);

    Optional<CaptureIdentityLink> findByCaptureId(UUID captureId);

    Optional<CaptureIdentityLink> findBySubmissionUid(String submissionUid);
}
