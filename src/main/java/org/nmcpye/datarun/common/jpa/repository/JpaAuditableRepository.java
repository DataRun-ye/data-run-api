package org.nmcpye.datarun.common.jpa.repository;

import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface JpaAuditableRepository<T extends JpaAuditableObject>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T>, AuditableObjectRepository<T, Long> {
    void deleteByUid(String uid);

    boolean existsByUid(String uid);

    void deleteAllByUidIn(Collection<String> uids);

    Optional<T> findByUid(String uid);

    Set<T> findAllByUidIn(Collection<String> uids);

    Page<T> findAllByUidIn(Collection<String> uids, Pageable pageable);
}
