package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.orgunitgroup.OrgUnitGroup;
import org.nmcpye.datarun.team.Team;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitGroupService
    extends JpaAuditableObjectService<OrgUnitGroup> {
}
