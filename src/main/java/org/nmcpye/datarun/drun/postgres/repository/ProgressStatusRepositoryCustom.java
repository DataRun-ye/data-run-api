package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.ProgressStatus;
import org.nmcpye.datarun.repository.ProgressStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Custom repository for the ProgressStatus entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProgressStatusRepositoryCustom
    extends ProgressStatusRepository {
    void deleteByUid(String uid);

    Optional<ProgressStatus> findByUid(String uid);
}
