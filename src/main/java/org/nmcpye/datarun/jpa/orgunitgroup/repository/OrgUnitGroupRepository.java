package org.nmcpye.datarun.jpa.orgunitgroup.repository;

import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.jpa.orgunitgroup.OrgUnitGroup;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitGroupRepository
    extends JpaAuditableRepository<OrgUnitGroup> {

    Optional<OrgUnitGroup> findByCode(String code);
}
