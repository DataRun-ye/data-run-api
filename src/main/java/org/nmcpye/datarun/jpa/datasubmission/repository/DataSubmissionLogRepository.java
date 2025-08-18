package org.nmcpye.datarun.jpa.datasubmission.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmissionLog;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
public interface DataSubmissionLogRepository
    extends BaseJpaIdentifiableRepository<DataSubmissionLog, Long> {
    List<DataSubmissionLog> findBySubmissionIdOrderByVersionNoDesc(String submissionId);
}
