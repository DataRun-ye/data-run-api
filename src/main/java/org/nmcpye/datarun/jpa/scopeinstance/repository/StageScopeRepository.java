package org.nmcpye.datarun.jpa.scopeinstance.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.scopeinstance.StageContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing StageScope entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
public interface StageScopeRepository
    extends BaseJpaIdentifiableRepository<StageContext, String> {
    /**
     * Find StageScope by its ID (which is also the stageSubmissionId)
     *
     * @param stageSubmissionId stage submission
     * @return Found StageScope
     */
    Optional<StageContext> findByStageSubmissionId(String stageSubmissionId);

    /**
     * Find StageScope by associated entity instance ID
     *
     * @param entityInstanceId entity Instance id
     * @return List of found StageScopes
     */
    List<StageContext> findByEntityInstanceId(String entityInstanceId);
}
