package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitGroupSetService
    extends IdentifiableRelationalService<OrgUnitGroupSet> {
    @Override
    default Specification<OrgUnitGroupSet> canRead() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
}
