package org.nmcpye.datarun.jpa.flowinstance.service;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.exception.ScopeValidationException;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeInstance;

import java.util.Map;
import java.util.Optional;

/**
 * @author Hamza Assada 20/03/2023
 */
public interface FlowInstanceService
    extends JpaIdentifiableObjectService<FlowInstance> {

    /**
     * Creates a new FlowInstance based on a FlowType and initial scope data.
     * This method is transactional and handles validation.
     *
     * @param flowTypeId  The ID of the FlowType template to use.
     * @param scopeValues A map containing the initial scope data (e.g., orgUnitId, scopeDate, custom fields).
     * @return The newly created and persisted FlowInstance.
     * @throws EntityNotFoundException  if the FlowType is not found.
     * @throws ScopeValidationException if the provided scopeData fails validation.
     */
    FlowInstance createFlowInstance(String flowTypeId, Map<String, Object> scopeValues);

    /**
     * Retrieves the current (root) ScopeInstance for a given FlowInstance.
     * In this design, a FlowInstance has exactly one root ScopeInstance.
     *
     * @param flowInstanceId The ID of the FlowInstance.
     * @return An Optional containing the ScopeInstance if found.
     */
    Optional<ScopeInstance> getCurrentScopeForFlow(String flowInstanceId);
}
