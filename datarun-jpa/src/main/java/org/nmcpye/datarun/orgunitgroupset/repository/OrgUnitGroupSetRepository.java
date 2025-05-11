package org.nmcpye.datarun.orgunitgroupset.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet;
import org.nmcpye.datarun.orgunit.repository.OrgUnitRepositoryWithBagRelationships;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitGroupSetRepository
    extends OrgUnitRepositoryWithBagRelationships,
    JpaAuditableRepository<OrgUnitGroupSet> {

    Optional<OrgUnitGroupSet> findByCode(String code);
}
