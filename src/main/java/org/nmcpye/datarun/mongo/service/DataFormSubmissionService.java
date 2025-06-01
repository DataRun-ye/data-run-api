package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.common.FindExistingSubmissionsDto;
import org.nmcpye.datarun.common.impl.SoftDeleteService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionService
    extends SoftDeleteService<DataFormSubmission, String> {

    FindExistingSubmissionsDto findExistingAndMissingOrgUnitCodes(List<String> codes, String form);

    void findAndFixFormDataSerialNumbers();
}
