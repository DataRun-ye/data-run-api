package org.nmcpye.datarun.drun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataFormSubmission entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataFormSubmissionRepositoryCustom
    extends IdentifiableMongoRepository<DataFormSubmission> {

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmission> findAllByUser(Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmission> findAllWithEagerRelationshipsByUser(Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    List<DataFormSubmission> findAllWithEagerRelationshipsByUser();

    @Query("{'id': ?0, 'team.userInfo.login' : ?#{authentication.name}}")
    Optional<DataFormSubmission> findOneWithEagerRelationshipsByUser(String id);
}
