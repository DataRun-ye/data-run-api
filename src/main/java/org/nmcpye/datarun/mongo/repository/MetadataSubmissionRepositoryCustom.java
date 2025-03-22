package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.common.mongo.repository.MongoAuditableRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.MetadataSubmission;
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
public interface MetadataSubmissionRepositoryCustom
    extends MongoAuditableRepository<MetadataSubmission> {

    @Query("{ 'serialNumber' : { $exists: false } }")
    List<DataFormSubmission> findBySerialNumberNull();

    Page<MetadataSubmission> findBySerialNumberGreaterThan(Long serialNumber, Pageable pageable);

    Page<MetadataSubmission> findBySerialNumberGreaterThanAndMetadataSchema(Long serialNumber, String metadataSchema, Pageable pageable);

    @Query("{resourceType: ?0, resourceId :  ?0, metadataSchema: ?0}")
    Page<MetadataSubmission> findAllByResourceTypeAndResourceIdAndAndMetadataSchema(String resourceType, String resourceId, String metadataSchema, Pageable pageable);

    @Query("{'resourceId' :  ?0}")
    Page<MetadataSubmission> findAllByResourceId(String resourceId, Pageable pageable);


    @Query("{'metadataSchema': ?0}")
    Page<DataFormSubmission> findAllByMetadataSchema(String metadataSchema, Pageable pageable);

}
