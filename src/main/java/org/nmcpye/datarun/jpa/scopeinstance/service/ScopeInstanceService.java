package org.nmcpye.datarun.jpa.scopeinstance.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeInstance;

import java.util.Map;

public interface ScopeInstanceService
    extends JpaIdentifiableObjectService<ScopeInstance> {
    /**
     * Creates or update scope instance
     *
     * @param flowInstanceId flowInstance id this scope is part of
     * @param scopeElements  scope elements
     * @return created ScopeInstance
     */
    ScopeInstance createForFlow(String flowInstanceId, Map<String, Object> scopeElements);

    /**
     * Create or update scope instance
     *
     * @param flowInstanceId    flowInstance id this scope is part of
     * @param stageSubmissionId stage submission id this scope is part of
     * @param scopeElements     scope elements
     * @param entityInstanceId  entity instance id (a stage bound entity id)
     * @return created scopeInstance
     */
    ScopeInstance createForStage(String flowInstanceId, String stageSubmissionId,
                                 Map<String, String> scopeElements, String entityInstanceId);
}
