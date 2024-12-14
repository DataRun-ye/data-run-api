package org.nmcpye.datarun.common;

import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface IdentifiableRepository<T extends Identifiable<ID>, ID>
    extends CrudRepository<T, ID> {
    void deleteByUid(String uid);

    void deleteAllByUidIn(Iterable<String> uids);

    Optional<T> findByUid(String uid);

    Set<T> findAllByUidIn(Collection<String> uids);

//    Set<T> findAllByCodeIn(Collection<String> codes);
}
