package org.nmcpye.datarun.drun.mongo.repository;

import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@SuppressWarnings("unused")
@NoRepositoryBean
public interface IdentifiableMongoRepository<T extends IdentifiableObject<String>>
    extends MongoRepository<T, String> {
    @Query("{'uid': ?0}")
    Optional<T> findByUid(String uid);

    @Query("{'code': ?0}")
    Optional<T> findByCode(String code);

//    Page<T> findAllByUser(Pageable pageable);
//
//    Page<T> findAllWithEagerRelationshipsByUser(Pageable pageable);
//
//    List<T> findAllWithEagerRelationshipsByUser();
//
//    Optional<T> findOneWithEagerRelationshipsByUser(String id);
}
