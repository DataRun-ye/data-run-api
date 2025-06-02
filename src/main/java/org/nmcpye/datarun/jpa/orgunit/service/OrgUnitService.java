package org.nmcpye.datarun.jpa.orgunit.service;

import org.nmcpye.datarun.jpa.common.JpaAuditableObjectService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;

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
