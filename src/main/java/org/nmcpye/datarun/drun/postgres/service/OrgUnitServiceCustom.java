package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.common.OrgUnitSpecification;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitServiceCustom
    extends IdentifiableRelationalService<OrgUnit> {

    @Override
    default Specification<OrgUnit> canRead() {
        return OrgUnitSpecification.canRead();
    }

    Set<OrgUnit> getUserTeamsOrganisationUnits();

    Set<OrgUnit> getUserManagedTeamsOrganisationUnits();

    Set<OrgUnit> getAllUserAccessibleOrganisationUnits();

    void updatePaths();

    void forceUpdatePaths();
}
