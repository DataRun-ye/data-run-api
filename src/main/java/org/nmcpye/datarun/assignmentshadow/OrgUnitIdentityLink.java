package org.nmcpye.datarun.assignmentshadow;

import java.util.UUID;

public record OrgUnitIdentityLink(
    UUID orgUnitId,
    String baselineOrgUnitUid
) {
}
