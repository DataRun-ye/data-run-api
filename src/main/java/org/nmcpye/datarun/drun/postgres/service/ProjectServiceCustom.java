package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Service Interface for managing {@link Project}.
 */
public interface ProjectServiceCustom
    extends IdentifiableRelationalService<Project> {
    @Override
    default Specification<Project> canRead() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
}
