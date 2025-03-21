package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaIdentifiableRepository;
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
    JpaIdentifiableRepository<OrgUnitGroup> {

    Optional<OrgUnitGroup> findByCode(String code);
}
