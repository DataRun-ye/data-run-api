package org.nmcpye.datarun.jpa.datasubmission.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmissionHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
public interface DataSubmissionHistoryRepository
    extends BaseJpaIdentifiableRepository<DataSubmissionHistory, Long> {
    List<DataSubmissionHistory> findBySubmissionIdOrderByVersionNoDesc(String submissionId);

    //
    // Use a pageable query to get most recent N history rows for a submission
    @Query("select h from DataSubmissionHistory h where h.submissionId = :submissionId order by h.createdAt desc")
    List<DataSubmissionHistory> findBySubmissionIdOrderByCreatedAtDesc(@Param("submissionId") String submissionId, Pageable pageable);

    // Delete all history rows for a submission (used if keepIds is empty)
    @Modifying
    @Transactional
    void deleteBySubmissionId(String submissionId);

    // Delete rows for submission whose id is NOT in keepIds
    @Modifying
    @Transactional
    @Query("delete from DataSubmissionHistory h where h.submissionId = :submissionId and h.id not in :keepIds")
    void deleteOlderThanKeepIds(@Param("submissionId") String submissionId, @Param("keepIds") List<Long> keepIds);
}
