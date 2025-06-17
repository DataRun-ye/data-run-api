package org.nmcpye.datarun.jpa.orgunit.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;

/**
 * Service Interface for managing {@link OrgUnit}.
 *
 * @author Hamza Assada 18/01/2022
 */
public interface OrgUnitService
    extends JpaIdentifiableObjectService<OrgUnit> {

//    Set<OrgUnit> getUserTeamsOrganisationUnits();

//    Set<OrgUnit> getUserManagedTeamsOrganisationUnits();

//    Set<OrgUnit> getAllUserAccessibleOrganisationUnits();

    void updatePaths();

    void forceUpdatePaths();
}
