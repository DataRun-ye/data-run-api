package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface OrgUnitRepositoryWithBagRelationships {
    Optional<OrgUnit> fetchBagRelationships(Optional<OrgUnit> orgUnit);

    List<OrgUnit> fetchBagRelationships(List<OrgUnit> orgUnits);

    Page<OrgUnit> fetchBagRelationships(Page<OrgUnit> orgUnits);

    void updatePaths();

    void forceUpdatePaths();
}
