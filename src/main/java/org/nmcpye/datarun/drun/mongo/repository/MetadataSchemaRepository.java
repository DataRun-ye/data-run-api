package org.nmcpye.datarun.drun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.domain.MetadataSchema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the DataFormSubmission entity.
 */
@Repository
@JaversSpringDataAuditable
public interface MetadataSchemaRepository
    extends IdentifiableMongoRepository<MetadataSchema> {
    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmission> findAllByUser(Pageable pageable);

    @Query("{resourceType: ?0, resourceId :  ?0}")
    Page<MetadataSchema> findAllByResourceTypeAndResourceId(String resourceType, String resourceId, Pageable pageable);

    @Query("{'resourceId' :  ?0}")
    Page<MetadataSchema> findAllByResourceId(String resourceId, Pageable pageable);
}
