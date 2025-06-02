package org.nmcpye.datarun.mongo.common.repository;

import org.nmcpye.datarun.mongo.common.MongoAuditableBaseObject;
import org.nmcpye.datarun.common.AuditableObjectRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@SuppressWarnings("unused")
@NoRepositoryBean
public interface MongoAuditableRepository<T extends MongoAuditableBaseObject>
    extends MongoRepository<T, String>, AuditableObjectRepository<T, String> {
    @Query("{'uid': ?0}")
    Optional<T> findByUid(String uid);
}
