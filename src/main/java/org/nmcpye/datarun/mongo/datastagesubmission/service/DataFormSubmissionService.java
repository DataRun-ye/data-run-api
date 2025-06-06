package org.nmcpye.datarun.mongo.datastagesubmission.service;

import org.nmcpye.datarun.common.FindExistingSubmissionsDto;
import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionService
    extends IdentifiableObjectService<DataFormSubmission, String> {

    FindExistingSubmissionsDto findExistingAndMissingOrgUnitCodes(List<String> codes, String form);

    void findAndFixFormDataSerialNumbers();
}
