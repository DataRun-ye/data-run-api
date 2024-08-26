package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface IdentifiableRelationalRepository<T extends IdentifiableObject<Long>>
    extends JpaRepository<T, Long> {
    Optional<T> findByUid(String uid);

    Optional<T> findByCode(String uid);

    List<T> findAllByUidIn(Collection<String> uids);

    List<T> findAllByCodeIn(Collection<String> codes);
}
