package org.nmcpye.datarun.drun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.drun.mongo.domain.AssignmentMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the Assignment entity.
 */
@Repository
@JaversSpringDataAuditable
public interface AssignmentRepositoryCustom
    extends IdentifiableMongoRepository<AssignmentMongo> {

    @Query("{}")
    Page<AssignmentMongo> findAllByUser(Pageable pageable);

    @Query("{}")
    Page<AssignmentMongo> findAllWithEagerRelationshipsByUser(Pageable pageable);

    @Query("{}")
    List<AssignmentMongo> findAllWithEagerRelationshipsByUser();

    @Query("{'id': ?0}")
    Optional<AssignmentMongo> findOneWithEagerRelationshipsByUser(String id);
}
