package org.nmcpye.datarun.orgunitgroupset.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.orgunitgroupset.OrgUnitGroupSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitGroupSetRepository
    extends JpaAuditableRepository<OrgUnitGroupSet> {

    Optional<OrgUnitGroupSet> findByCode(String code);
}
