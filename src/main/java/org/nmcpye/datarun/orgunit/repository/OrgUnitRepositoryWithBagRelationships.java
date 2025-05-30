package org.nmcpye.datarun.orgunit.repository;

public interface OrgUnitRepositoryWithBagRelationships {
//    Optional<OrgUnit> fetchBagRelationships(Optional<OrgUnit> orgUnit);

//    List<OrgUnit> fetchBagRelationships(List<OrgUnit> orgUnits);

//    Page<OrgUnit> fetchBagRelationships(Page<OrgUnit> orgUnits);

    void updatePaths();

    void forceUpdatePaths();
}
