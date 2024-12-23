package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.mongo.domain.MetadataSubmissionUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataFormSubmission entity.
 */
@Repository
@JaversSpringDataAuditable
public interface MetadataSubmissionUpdateRepositoryCustom
    extends IdentifiableMongoRepository<MetadataSubmissionUpdate> {

    @Query("{ 'serialNumber' : { $exists: false } }")
    List<MetadataSubmissionUpdate> findBySerialNumberNull();

    @Query("{resourceType: ?0, resourceId :  ?0, metadataSchema: ?0}")
    Page<MetadataSubmissionUpdate> findAllByResourceTypeAndResourceIdAndAndMetadataSchema(String resourceType, String resourceId, String metadataSchema, Pageable pageable);

    @Query("{'resourceId' :  ?0}")
    Page<MetadataSubmissionUpdate> findAllByResourceId(String resourceId, Pageable pageable);


    @Query("{'metadataSchema': ?0}")
    Page<MetadataSubmissionUpdate> findAllByMetadataSchema(String metadataSchema, Pageable pageable);

}
