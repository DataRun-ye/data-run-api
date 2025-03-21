package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoIdentifiableService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;

import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionHistoryService
    extends DefaultMongoIdentifiableService<DataFormSubmissionHistory> {
    List<DataFormSubmissionHistory> getSubmissionVersions(String dataSubmissionId);

//    Page<DataFormSubmissionHistory> findAllByForm(List<String> forms, Pageable pageable, boolean includeDeleted);
}
