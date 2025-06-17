package org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator;

import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowtype.FlowType;

import java.util.Map;

/**
 * Defines the contract for creating a FlowInstance.
 * Each implementation handles a specific PlanningMode.
 *
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
public interface FlowInstanceCreationStrategy {
    FlowType.PlanningMode getPlanningMode();

    FlowInstance create(FlowType flowType, Map<String, Object> scopeValues);
}
