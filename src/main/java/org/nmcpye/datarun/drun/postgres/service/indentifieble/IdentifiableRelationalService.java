package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface IdentifiableRelationalService
    <T extends Identifiable<Long>>
    extends IdentifiableService<T, Long> {

    Specification<T> canRead();

    Optional<T> findIdentifiable(Identifiable<Long> identifiableObject);
}
