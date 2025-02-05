package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitGroupSetRelationalRepository
    extends OrgUnitRepositoryWithBagRelationships,
    IdentifiableRelationalRepository<OrgUnitGroupSet> {

    Optional<OrgUnitGroupSet> findByCode(String code);
}
