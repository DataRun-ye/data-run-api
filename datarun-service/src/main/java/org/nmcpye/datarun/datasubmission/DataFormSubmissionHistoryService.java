package org.nmcpye.datarun.datasubmission;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;

import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionHistoryService
    extends AuditableObjectService<DataFormSubmissionHistory, String> {
    List<DataFormSubmissionHistory> getSubmissionVersions(String dataSubmissionId);
}
