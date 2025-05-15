package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.FindExistingSubmissionsDto;
import org.nmcpye.datarun.common.impl.SoftDeleteObjectDelete;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionService
    extends AuditableObjectService<DataFormSubmission, String>, SoftDeleteObjectDelete<DataFormSubmission, String> {

    FindExistingSubmissionsDto findExistingAndMissingOrgUnitCodes(List<String> codes, String form);

    void findAndFixFormDataSerialNumbers();
}
