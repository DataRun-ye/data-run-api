package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitGroupRepository
    extends OrgUnitRepositoryWithBagRelationships,
    JpaAuditableRepository<OrgUnitGroup> {

    Optional<OrgUnitGroup> findByCode(String code);
}
