package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionService
    extends AuditableObjectService<DataFormSubmission, String> {

    DataFormSubmission saveVersioning(DataFormSubmission submission);
    void findAndFixFormDataSerialNumbers();
}
