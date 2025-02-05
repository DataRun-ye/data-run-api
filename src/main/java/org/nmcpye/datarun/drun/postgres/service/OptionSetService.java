package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Service Interface for managing {@link OptionSet}.
 */
public interface OptionSetService
    extends IdentifiableRelationalService<OptionSet> {
    @Override
    default Specification<OptionSet> canRead() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
}
