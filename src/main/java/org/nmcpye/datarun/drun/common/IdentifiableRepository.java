package org.nmcpye.datarun.drun.common;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface IdentifiableRepository<T extends IdentifiableObject<ID>, ID>
    extends CrudRepository<T, ID> {
    void deleteByUid(String uid);

    void deleteAllByUidIn(Iterable<String> uids);

    Optional<T> findByUid(String uid);

    Optional<T> findByCode(String code);

    Set<T> findAllByUidIn(Collection<String> uids);

    Set<T> findAllByCodeIn(Collection<String> codes);
}
