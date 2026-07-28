package org.nmcpye.datarun.assignmentshadow;

import java.util.Optional;
import java.util.UUID;

public interface OrgUnitIdentityLinkPort {

    OrgUnitIdentityLink insert(OrgUnitIdentityLink identityLink);

    Optional<OrgUnitIdentityLink> findByOrgUnitId(UUID orgUnitId);

    Optional<OrgUnitIdentityLink> findByBaselineOrgUnitUid(String baselineOrgUnitUid);
}
