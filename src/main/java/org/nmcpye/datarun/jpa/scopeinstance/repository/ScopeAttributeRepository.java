package org.nmcpye.datarun.jpa.scopeinstance.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.scopeinstance.FlowContext;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeAttribute;
import org.nmcpye.datarun.jpa.scopeinstance.StageContext;
import org.nmcpye.datarun.jpa.scopeinstance.WorkflowContext;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing ScopeAttribute entities (EAV).
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
public interface ScopeAttributeRepository
    extends BaseJpaIdentifiableRepository<ScopeAttribute, String> {
    /**
     * Find all ScopeAttributes for a given FlowScope (using its ID as FK)
     *
     * @param flowInstanceId flow instance id
     * @return List of found ScopeAttributes
     */
    List<ScopeAttribute> findByFlowScope_FlowInstanceId(String flowInstanceId);

    /**
     * Find all ScopeAttributes for a given StageScope (using its ID as FK)
     *
     * @param stageSubmissionId stage submission id
     * @return List of found ScopeAttributes
     */
    List<ScopeAttribute> findByStageScope_StageSubmissionId(String stageSubmissionId);

    /**
     * Find ScopeAttributes by type, key, and value
     *
     * @param scopeType scope type
     * @param key       key
     * @param value     value
     * @return List of found ScopeAttributes
     */
    List<ScopeAttribute> findByScopeTypeAndKeyAndValue(WorkflowContext.ScopeType scopeType, String key, String value);

    /**
     * Find ScopeAttributes for a specific key within a flow scope
     *
     * @param flowInstanceId flow instance id
     * @param key            key
     * @return List of found ScopeAttributes
     */
    List<ScopeAttribute> findByFlowScope_FlowInstanceIdAndKey(String flowInstanceId, String key);

    /**
     * Find ScopeAttributes for a specific key within a stage scope
     *
     * @param stageSubmissionId stage submission id
     * @param key               key
     * @return List of found ScopeAttributes
     */
    List<ScopeAttribute> findByStageScope_StageSubmissionIdAndKey(String stageSubmissionId, String key);

    /**
     * Custom query to fetch all attributes associated with a specific FlowScope object
     *
     * @param flowContext flow scope
     * @return List of found ScopeAttributes
     */
    @Query("SELECT sa FROM ScopeAttribute sa WHERE sa.flowScope = :flowScope")
    List<ScopeAttribute> findByFlowScope(FlowContext flowContext);

    /**
     * Custom query to fetch all attributes associated with a specific StageScope object
     *
     * @param stageContext stage scope
     * @return List of found ScopeAttributes
     */
    @Query("SELECT sa FROM ScopeAttribute sa WHERE sa.stageScope = :stageScope")
    List<ScopeAttribute> findByStageScope(StageContext stageContext);
}
