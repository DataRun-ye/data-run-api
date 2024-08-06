package org.nmcpye.datarun.drun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.drun.mongo.domain.OrganizationUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataForm entity.
 */
@Repository
@JaversSpringDataAuditable
public interface OrganizationUnitRepositoryCustom
    extends IdentifiableMongoRepository<OrganizationUnit> {

    @Query("{}")
    Page<OrganizationUnit> findAllByUser(Pageable pageable);

    @Query("{}")
    Page<OrganizationUnit> findAllWithEagerRelationshipsByUser(Pageable pageable);

    @Query("{}")
    List<OrganizationUnit> findAllWithEagerRelationshipsByUser();

    @Query("{'id': ?0}")
    Optional<OrganizationUnit> findOneWithEagerRelationshipsByUser(String id);
}
