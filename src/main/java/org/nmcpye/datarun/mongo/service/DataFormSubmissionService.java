package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionService
    extends IdentifiableMongoService<DataFormSubmission> {

    DataFormSubmission saveVersioning(DataFormSubmission submission);

//    Page<DataFormSubmission> findAllByForm(List<String> forms, Pageable pageable, boolean includeDeleted);

//    Page<DataFormSubmission> findSubmissionsBySerialNumber(Long serialNumber, String form, Pageable pageable, boolean includeDeleted);
}
