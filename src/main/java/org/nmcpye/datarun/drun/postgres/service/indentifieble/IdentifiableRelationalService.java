package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import org.nmcpye.datarun.common.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface IdentifiableRelationalService
    <T extends IdentifiableEntity<Long>>
    extends IdentifiableService<T, Long> {

    Specification<T> canRead();

    default Specification<T> appendReadSpecification(Specification<T> spec) {
        return spec == null ? canRead() : canRead().and(spec);
    }

    Page<T> findAllByUser(Specification<T> spec, Pageable pageable);

    Optional<T> findIdentifiable(IdentifiableEntity<Long> identifiableObject);
}
