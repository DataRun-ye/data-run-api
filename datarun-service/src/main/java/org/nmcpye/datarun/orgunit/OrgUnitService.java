package org.nmcpye.datarun.orgunit;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;

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
