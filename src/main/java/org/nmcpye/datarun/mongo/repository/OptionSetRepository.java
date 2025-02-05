//package org.nmcpye.datarun.mongo.repository;
//
//import org.javers.spring.annotation.JaversSpringDataAuditable;
//import org.nmcpye.datarun.mongo.domain.OptionSet;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
///**
// * Spring Data MongoDB repository for the OptionSet entity.
// */
//@Repository
//@JaversSpringDataAuditable
//public interface OptionSetRepository
//    extends IdentifiableMongoRepository<OptionSet> {
//    Optional<OptionSet> findFirstByNameIgnoreCase(String name);
//}
