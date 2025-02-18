package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionService
    extends IdentifiableMongoService<DataFormSubmission> {

    DataFormSubmission saveVersioning(DataFormSubmission submission);
    void findAndFixFormDataSerialNumbers();
}
