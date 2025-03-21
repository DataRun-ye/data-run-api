package org.nmcpye.datarun.common.repository;

import org.nmcpye.datarun.common.IdentifiableObject;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IdentifiableRepository<T extends IdentifiableObject<ID>, ID>
    extends AuditableObjectRepository<T, ID> {
}
