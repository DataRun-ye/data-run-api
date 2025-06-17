package org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator;

import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.springframework.stereotype.Service;

import static org.nmcpye.datarun.jpa.flowtype.FlowType.PlanningMode.PLANNED;

/**
 * select type of flow based on FlowType.planningMode
 *
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
@Service
public class FlowInstanceCreatorFactory {
    private final PlannedFlowInstanceStrategy planned;
    private final LogAsYouGoFlowCreationStrategy logGo;
    public FlowInstanceCreatorFactory(PlannedFlowInstanceStrategy p, LogAsYouGoFlowCreationStrategy l) {
        this.planned = p; this.logGo = l;
    }
    public FlowInstanceCreationStrategy get(FlowType.PlanningMode mode) {
        return mode == PLANNED ? planned : logGo;
    }
}
