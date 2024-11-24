package org.nmcpye.datarun.drun.mongo.service;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.domain.MetadataSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface MetadataSubmissionService
    extends IdentifiableMongoService<MetadataSubmission> {

    Page<MetadataSubmission> findAllByForm(List<String> forms, Pageable pageable);

    Page<MetadataSubmission> findAllByEntity(List<String> entityUids, Pageable pageable);
    Page<MetadataSubmission> findAllByResourceType(String resourceType, Pageable pageable);
    Page<MetadataSubmission> findAllByEntity(String uid, Pageable pageable);

    Page<MetadataSubmission> findSubmissionsBySerialNumber(Long serialNumber, String form, Pageable pageable);
}
