package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitGroupService
    extends IdentifiableRelationalService<OrgUnitGroup> {

    @Override
    default Specification<OrgUnitGroup> canRead() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

}
