package org.nmcpye.datarun.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface AuditableObjectRepository<T extends AuditableObject<ID>, ID>
    extends CrudRepository<T, ID> {
    void deleteByUid(String uid);

    boolean existsByUid(String uid);

    void deleteAllByUidIn(Collection<String> uids);

    Optional<T> findByUid(String uid);

    Set<T> findAllByUidIn(Collection<String> uids);

    Page<T> findAllByUidIn(Collection<String> uids, Pageable pageable);
}
