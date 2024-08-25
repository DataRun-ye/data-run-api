package org.nmcpye.datarun.drun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataForm entity.
 */
@Repository
@JaversSpringDataAuditable
public interface ReferenceRepository
    extends IdentifiableMongoRepository<DataForm> {

    List<DataForm> findAllByActivity(String activity);


//    @Query("{}")
//    Page<DataForm> findAllWithEagerRelationshipsByUser(Pageable pageable);
//
//    @Query("{}")
//    List<DataForm> findAllWithEagerRelationshipsByUser();
//
//    @Query("{'id': ?0}")
//    Optional<DataForm> findOneWithEagerRelationshipsByUser(String id);
}
