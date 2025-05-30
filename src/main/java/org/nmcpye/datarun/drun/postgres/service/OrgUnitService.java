package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.orgunit.OrgUnit;
import org.nmcpye.datarun.team.Team;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitService
    extends JpaAuditableObjectService<OrgUnit> {

//    Set<OrgUnit> getUserTeamsOrganisationUnits();

//    Set<OrgUnit> getUserManagedTeamsOrganisationUnits();

//    Set<OrgUnit> getAllUserAccessibleOrganisationUnits();

    void updatePaths();

    void forceUpdatePaths();
}
