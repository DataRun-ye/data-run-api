package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
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

    @Query("{ 'serialNumber' : { $exists: false } }")
    List<DataFormSubmission> findBySerialNumberNull();

    Page<DataFormSubmission> findBySerialNumberGreaterThan(Long serialNumber, Pageable pageable);

    Page<DataFormSubmission> findBySerialNumberGreaterThanAndForm(Long serialNumber, String form, Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmission> findAllByUser(Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmission> findAllWithEagerRelationshipsByUser(Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    List<DataFormSubmission> findAllWithEagerRelationshipsByUser();

    @Query("{'id': ?0, 'team.userInfo.login' : ?#{authentication.name}}")
    Optional<DataFormSubmission> findOneWithEagerRelationshipsByUser(String id);

    @Query("{'form': ?0}")
    Page<DataFormSubmission> findAllByForm(String form, Pageable pageable);

    List<DataFormSubmission> findAllByTeamIn(List<String> teams);

}
