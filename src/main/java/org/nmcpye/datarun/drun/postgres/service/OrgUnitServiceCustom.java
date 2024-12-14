package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;

import java.util.Set;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitServiceCustom
    extends IdentifiableRelationalService<OrgUnit> {

    Set<OrgUnit> getUserTeamsOrganisationUnits();

    Set<OrgUnit> getUserManagedTeamsOrganisationUnits();

    Set<OrgUnit> getAllUserAccessibleOrganisationUnits();

    void updatePaths();

    void forceUpdatePaths();
}
