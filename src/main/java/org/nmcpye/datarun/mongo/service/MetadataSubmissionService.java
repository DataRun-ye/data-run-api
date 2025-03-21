package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.MetadataSubmission;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface MetadataSubmissionService
    extends AuditableObjectService<MetadataSubmission, String> {

//    Page<MetadataSubmission> findAllByForm(List<String> forms, Pageable pageable);

//    Page<MetadataSubmission> findAllByEntity(List<String> entityUids, Pageable pageable);
//    Page<MetadataSubmission> findAllByResourceType(String resourceType, Pageable pageable);
//    Page<MetadataSubmission> findAllByEntity(String uid, Pageable pageable);

//    Page<MetadataSubmission> findSubmissionsBySerialNumber(Long serialNumber, String form, Pageable pageable);
}
