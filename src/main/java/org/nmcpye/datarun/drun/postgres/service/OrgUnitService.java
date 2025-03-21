package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitService
    extends IdentifiableService<OrgUnit, Long> {

//    Set<OrgUnit> getUserTeamsOrganisationUnits();

//    Set<OrgUnit> getUserManagedTeamsOrganisationUnits();

//    Set<OrgUnit> getAllUserAccessibleOrganisationUnits();

    void updatePaths();

    void forceUpdatePaths();
}
