package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.domain.OuLevel;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Service Interface for managing {@link OuLevel}.
 */
public interface OuLevelServiceCustom extends IdentifiableRelationalService<OuLevel> {
    @Override
    default Specification<OuLevel> canRead() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
}
