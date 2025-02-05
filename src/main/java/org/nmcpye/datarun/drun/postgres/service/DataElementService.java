package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Service Interface for managing {@link DataElement}.
 */
public interface DataElementService
    extends IdentifiableRelationalService<DataElement> {
    @Override
    default Specification<DataElement> canRead() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
}
