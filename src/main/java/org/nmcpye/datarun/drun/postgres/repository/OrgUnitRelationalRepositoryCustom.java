package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.OrgUnit;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitRelationalRepositoryCustom
    extends IdentifiableRelationalRepository<OrgUnit> {
}
