package org.nmcpye.datarun.common.mongo.repository;

import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.common.repository.IdentifiableRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@SuppressWarnings("unused")
@NoRepositoryBean
public interface MongoIdentifiableRepository<T extends IdentifiableObject<String>>
    extends MongoRepository<T, String>, IdentifiableRepository<T, String> {
    @Query("{'uid': ?0}")
    Optional<T> findByUid(String uid);
}
