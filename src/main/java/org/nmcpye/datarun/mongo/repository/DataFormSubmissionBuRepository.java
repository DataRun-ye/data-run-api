package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.common.mongo.repository.MongoIdentifiableRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataFormSubmissionBu entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataFormSubmissionBuRepository
    extends MongoIdentifiableRepository<DataFormSubmissionBu> {

    @Query("{ 'serialNumber' : { $exists: false } }")
    List<DataFormSubmissionBu> findBySerialNumberNull();

    Page<DataFormSubmissionBu> findBySerialNumberGreaterThan(Long serialNumber, Pageable pageable);

    Page<DataFormSubmissionBu> findBySerialNumberGreaterThanAndForm(Long serialNumber, String form, Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmissionBu> findAllByUser(Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmissionBu> findAllWithEagerRelationshipsByUser(Pageable pageable);

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    List<DataFormSubmissionBu> findAllWithEagerRelationshipsByUser();

    @Query("{'id': ?0, 'team.userInfo.login' : ?#{authentication.name}}")
    Optional<DataFormSubmissionBu> findOneWithEagerRelationshipsByUser(String id);

    @Query("{'form': ?0}")
    Page<DataFormSubmissionBu> findAllByForm(String form, Pageable pageable);

    List<DataFormSubmissionBu> findAllByTeamIn(List<String> teams);

}
