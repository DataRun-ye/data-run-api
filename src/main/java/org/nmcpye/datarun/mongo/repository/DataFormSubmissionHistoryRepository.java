package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataFormSubmission entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataFormSubmissionHistoryRepository
    extends IdentifiableMongoRepository<DataFormSubmissionHistory> {

    List<DataFormSubmissionHistory> findAllByDataSubmissionId(String dataSubmissionId);
}
