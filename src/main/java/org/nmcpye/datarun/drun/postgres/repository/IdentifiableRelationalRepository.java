package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.common.IdentifiableRepository;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface IdentifiableRelationalRepository<T extends IdentifiableObject<Long>>
    extends JpaRepository<T, Long>,
    JpaSpecificationExecutor<T>, IdentifiableRepository<T, Long> {
    Optional<T> findByUid(String uid);

    Optional<T> findByCode(String uid);

    Set<T> findAllByUidIn(Collection<String> uids);

    Set<T> findAllByCodeIn(Collection<String> codes);
}
