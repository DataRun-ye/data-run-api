package org.nmcpye.datarun.common.jpa.repository;

import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.common.repository.IdentifiableRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface JpaIdentifiableRepository<T extends JpaBaseIdentifiableObject>
    extends JpaAuditableRepository<T>, IdentifiableRepository<T, Long> {
}
