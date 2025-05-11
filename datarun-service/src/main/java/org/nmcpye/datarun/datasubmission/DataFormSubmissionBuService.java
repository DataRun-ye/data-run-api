package org.nmcpye.datarun.datasubmission;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionBuService
    extends AuditableObjectService<DataFormSubmissionBu, String> {
}
