package org.nmcpye.datarun.jpa.orgunitgroup.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.orgunitgroup.OrgUnitGroup;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitGroupRepository
    extends JpaIdentifiableRepository<OrgUnitGroup> {

    Optional<OrgUnitGroup> findByCode(String code);
}
