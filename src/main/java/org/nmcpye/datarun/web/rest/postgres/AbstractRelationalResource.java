package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/custom")
public abstract class
AbstractRelationalResource<T extends IdentifiableObject<Long>>
    extends AbstractResource<T, Long> {

    protected AbstractRelationalResource(IdentifiableRelationalService<T> identifiableService,
                                         JpaRepository<T, Long> repository) {
        super(identifiableService, repository);
    }
}
