package org.nmcpye.datarun.jpa.datasubmission.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmissionHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
public interface DataSubmissionHistoryRepository
    extends BaseJpaIdentifiableRepository<DataSubmissionHistory, Long> {
    List<DataSubmissionHistory> findBySubmissionIdOrderByVersionNoDesc(String submissionId);
}
