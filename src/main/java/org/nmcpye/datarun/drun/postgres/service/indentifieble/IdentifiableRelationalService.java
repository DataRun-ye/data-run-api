package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface IdentifiableRelationalService
    <T extends IdentifiableObject<Long>>
    extends IdentifiableService<T, Long> {

    Page<T> findAll(Specification<T> spec, Pageable pageable);

    List<T> findAll(Specification<T> spec);

    Optional<T> findOne(Specification<T> spec);

    Optional<T> findIdentifiable(IdentifiableObject<Long> identifiableObject);
}
