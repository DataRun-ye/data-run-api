package org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator;

import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.exception.FlowCreationException;
import org.nmcpye.datarun.jpa.flowinstance.repository.FlowInstanceRepository;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.jpa.scopeinstance.service.ScopeInstanceService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.nmcpye.datarun.jpa.flowtype.FlowType.PlanningMode.LOG_AS_YOU_GO;

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
@Service
public class LogAsYouGoFlowCreationStrategy implements FlowInstanceCreationStrategy {
    private final FlowTypeRepository ftRepo;
    private final FlowInstanceRepository fiRepo;
    private final EntityInstanceRepository eiRepo;
    private final ScopeInstanceService siSvc;

    public LogAsYouGoFlowCreationStrategy(FlowTypeRepository ftRepo, FlowInstanceRepository fiRepo, EntityInstanceRepository eiRepo, ScopeInstanceService siSvc) {
        this.ftRepo = ftRepo;
        this.fiRepo = fiRepo;
        this.eiRepo = eiRepo;
        this.siSvc = siSvc;
    }

    @Override
    public FlowType.PlanningMode getPlanningMode() {
        return LOG_AS_YOU_GO;
    }

    @Override
    @Transactional
    public FlowInstance create(String flowTypeId, Map<String, Object> ignored, CurrentUserDetails ctx) {
        FlowType ft = ftRepo.findById(flowTypeId)
            .orElseThrow(() -> new FlowCreationException("Unknown FlowType"));
        if (ft.getPlanningMode() != LOG_AS_YOU_GO)
            throw new FlowCreationException("Not LOG_AS_YOU_GO");
        String teamId = ctx.getUserTeamsUIDs().stream().findFirst()
            .orElseThrow(() -> new FlowCreationException("No active team"));

        // Validate entityInstance if needed
        if (scopes.containsKey("entityInstance")) {
            String eiId = scopes.get("entityInstance").toString();
            if (!eiRepo.existsById(eiId))
                throw new FlowCreationException("Invalid entityInstance");
        }

        Map<String, Object> scopes = Map.of(
            "team", teamId.toString(),
            "orgUnit", ctx.getOrgUnitId().toString(),
            "date", Instant.now().toString()
        );
        // Delegate to common create logic
        return new PlannedFlowInstanceStrategy(...).create(flowTypeId, scopes, ctx);
    }
}
