package org.nmcpye.datarun.mongo.datastagesubmission.repository;

import org.nmcpye.datarun.mongo.common.repository.MongoIdentifiableRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the DataFormSubmission entity.
 */
@Repository
//@JaversSpringDataAuditable
public interface DataFormSubmissionHistoryRepository
    extends MongoIdentifiableRepository<DataFormSubmissionHistory> {
}
