package org.nmcpye.datarun.common.mongo.repository;

import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
@NoRepositoryBean
public interface MongoAuditableRepository<T extends MongoAuditableBaseObject>
    extends MongoRepository<T, String>, AuditableObjectRepository<T, String> {
    @Query("{'uid': ?0}")
    Optional<T> findByUid(String uid);

    void deleteByUid(String uid);

    boolean existByUid(String uid);

    void deleteAllByUidIn(Collection<String> uids);


    Set<T> findAllByUidIn(Collection<String> uids);

    Page<T> findAllByUidIn(Collection<String> uids, Pageable pageable);
}
