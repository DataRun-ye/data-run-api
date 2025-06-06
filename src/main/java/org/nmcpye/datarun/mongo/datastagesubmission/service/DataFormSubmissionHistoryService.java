package org.nmcpye.datarun.mongo.datastagesubmission.service;

import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;

import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionHistoryService
    extends IdentifiableObjectService<DataFormSubmissionHistory, String> {
    List<DataFormSubmissionHistory> getSubmissionVersions(String dataSubmissionId);

    void saveToHistory(DataFormSubmission submission);

    void pruneOldVersions(String submissionId);
}
