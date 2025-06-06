package org.nmcpye.datarun.jpa.orgunitgroupset.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.orgunitgroupset.OrgUnitGroupSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitGroupSetRepository
    extends JpaIdentifiableRepository<OrgUnitGroupSet> {

    Optional<OrgUnitGroupSet> findByCode(String code);
}
