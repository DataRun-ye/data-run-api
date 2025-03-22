package org.nmcpye.datarun.common.repository;

import org.nmcpye.datarun.common.AuditableObject;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface AuditableObjectRepository<T extends AuditableObject<ID>, ID>
    extends CrudRepository<T, ID> {
//    void deleteByUid(String uid);
//
//    boolean existByUid(String uid);
//
//    void deleteAllByUidIn(Collection<String> uids);
//
//    Optional<T> findByUid(String uid);
//
//    Set<T> findAllByUidIn(Collection<String> uids);
//
//    Page<T> findAllByUidIn(Collection<String> uids, Pageable pageable);
}
