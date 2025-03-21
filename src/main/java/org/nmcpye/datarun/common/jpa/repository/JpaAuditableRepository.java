package org.nmcpye.datarun.common.jpa.repository;

import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface JpaAuditableRepository<T extends JpaAuditableObject>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T>, AuditableObjectRepository<T, Long> {
}
