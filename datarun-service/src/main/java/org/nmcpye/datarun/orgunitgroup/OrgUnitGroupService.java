package org.nmcpye.datarun.orgunitgroup;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.nmcpye.datarun.drun.postgres.domain.Team;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitGroupService
    extends JpaAuditableObjectService<OrgUnitGroup> {
}
