//package org.nmcpye.datarun.common.mongo.repository;
//
//import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.data.repository.NoRepositoryBean;
//
//import java.util.Optional;
//
//@SuppressWarnings("unused")
//@NoRepositoryBean
//public interface MongoIdentifiableRepository<T extends MongoBaseIdentifiableObject>
//    extends MongoRepository<T, String>, MongoAuditableRepository<T> {
//    @Query("{'uid': ?0}")
//    Optional<T> findByUid(String uid);
//}
