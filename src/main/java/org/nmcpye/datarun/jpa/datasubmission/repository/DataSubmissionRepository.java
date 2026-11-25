package org.nmcpye.datarun.jpa.datasubmission.repository;

import jakarta.persistence.LockModeType;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for data submissions.
 */
@Repository
public interface DataSubmissionRepository
    extends JpaIdentifiableRepository<DataSubmission> {

    /**
     * The stored submission with this uid, under the submission lock until the transaction ends: a pessimistic write
     * lock, which Hibernate issues on PostgreSQL as {@code SELECT ... FOR NO KEY UPDATE}, the lock every UPDATE of a
     * submission takes. A writer takes the submission lock as its first read, so its checks and its save see the
     * submission as locked, and another transaction's change of it waits until this one ends. A uid with no stored
     * submission locks nothing: a new submission's uid is kept unique by its unique constraint.
     *
     * <p>{@code FOR NO KEY UPDATE} rather than {@code FOR UPDATE}, and correctly so: no key column of a submission ever
     * changes (its id, uid and serial number are written once), so this lock keeps out every other writer of the
     * submission as {@code FOR UPDATE} would, while the journal's foreign-key checks on it ({@code KEY SHARE}, taken
     * when a revision is inserted) stay compatible. The waits are bounded by the transaction's {@code lock_timeout},
     * which {@code SubmissionLock} sets just before this read.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DataSubmission s WHERE s.uid = :uid")
    Optional<DataSubmission> findByUidForWrite(@Param("uid") String uid);

    @Override
    default Optional<DataSubmission> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default List<DataSubmission> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }


    @Override
    default Optional<DataSubmission> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<DataSubmission> findByNameLike(String name) {
        return Collections.emptyList();
    }

}
