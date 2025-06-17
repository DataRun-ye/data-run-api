package org.nmcpye.datarun.jpa.stagesubmission.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing StageSubmission entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
public interface StageSubmissionRepository
    extends JpaIdentifiableRepository<StageInstance> {
    // Find all submissions for a given flow instance
    List<StageInstance> findByFlowInstance(FlowInstance flowInstance);

    // Find submissions by flow instance and stage definition
    List<StageInstance> findByFlowInstanceAndStageDefinition(FlowInstance flowInstance, StageDefinition stageDefinition);

    // Find StageSubmissions for a FlowInstance and a given entityInstanceId (joining StageScope)
    @Query("SELECT ss FROM StageInstance ss JOIN ss.stageScope sc WHERE ss.flowInstance.id = :flowInstanceId AND sc.entityInstance.id = :entityInstanceId")
    List<StageInstance> findByFlowInstanceIdAndStageScopeEntityInstanceId(String flowInstanceId, String entityInstanceId);

    /**
     * All submissions for a given FlowInstance, ordered by submission time.
     */
    List<StageInstance> findByFlowInstanceIdOrderByCreatedDateAsc(String flowInstanceId);

    /**
     * All submissions for a given flow instance.
     */
    List<StageInstance> findByFlowInstanceId(String flowInstanceId);

    /**
     * Count submissions per step
     *
     * @param flowInstanceId id of flow instance
     * @return Count submissions per step
     */
    @Query("SELECT ss.stageDefinition.id, COUNT(ss) " +
        "FROM StageInstance ss " +
        "WHERE ss.flowInstance.id = :flowId " +
        "GROUP BY ss.stageDefinition.id")
    List<Object[]> countByStage(@Param("flowId") String flowInstanceId);
}
