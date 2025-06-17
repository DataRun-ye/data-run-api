package org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.exception.FlowCreationException;
import org.nmcpye.datarun.jpa.flowinstance.repository.FlowInstanceRepository;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.jpa.scopeinstance.DimensionalContext;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeInstance;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

import static org.nmcpye.datarun.jpa.flowtype.FlowType.PlanningMode.PLANNED;

//@Service
//public class PlannedFlowInstanceStrategy implements FlowInstanceCreationStrategy {
//    private final FlowTypeRepository ftRepo;
//    private final FlowInstanceRepository fiRepo;
//    private final EntityInstanceRepository eiRepo;
//    private final ScopeInstanceService siSvc;
//
//    public PlannedFlowInstanceStrategy(FlowTypeRepository ftRepo, FlowInstanceRepository fiRepo, EntityInstanceRepository eiRepo, ScopeInstanceService siSvc) {
//        this.ftRepo = ftRepo;
//        this.fiRepo = fiRepo;
//        this.eiRepo = eiRepo;
//        this.siSvc = siSvc;
//    }
//
//    @Override
//    public FlowType.PlanningMode getPlanningMode() {
//        return PLANNED;
//    }
//
//    @Override
//    @Transactional
//    public FlowInstance create(String flowTypeId, Map<String, Object> scopes, CurrentUserDetails ctx) {
//        FlowType ft = ftRepo.findById(flowTypeId)
//            .orElseThrow(() -> new FlowCreationException("Unknown FlowType"));
//        if (ft.getPlanningMode() != PLANNED)
//            throw new FlowCreationException("Not PLANNED mode");
//        // Validate user allowed
//        if (!ctx.getUserActiveFlowIds().contains(flowTypeId))
//            throw new FlowCreationException("User not allowed this flow");
//        // Validate scopes keys
//        for (var def : ft.getScopes()) {
//            if (def.isRequired() && !scopes.containsKey(def.getKey()))
//                throw new FlowCreationException("Missing scope " + def.getKey());
//        }
//        // Validate team
//        String teamId = scopes.get("team").toString();
//        if (!ctx.getUserTeamsUIDs().contains(teamId))
//            throw new FlowCreationException("Team not in user context");
//        // Validate entityInstance if needed
//        if (scopes.containsKey("entityInstance")) {
//            String eiId = scopes.get("entityInstance").toString();
//            if (!eiRepo.existsById(eiId))
//                throw new FlowCreationException("Invalid entityInstance");
//        }
//        // Create FlowInstance
//        FlowInstance fi = new FlowInstance();
//        fiRepo.save(fi);
//        // Create root ScopeInstance
//        siSvc.createForFlow(fi.getId(), scopes);
//        return fi;
//    }
//}

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
@Service
class PlannedFlowInstanceStrategy implements FlowInstanceCreationStrategy {

    private final ObjectMapper objectMapper;
    private final ScopeValidationService scopeValidationService;
    private final FlowTypeRepository ftRepo;
    private final FlowInstanceRepository fiRepo;

    public PlannedFlowInstanceStrategy(ObjectMapper objectMapper, ScopeValidationService scopeValidationService) {
        this.objectMapper = objectMapper;
        this.scopeValidationService = scopeValidationService;
    }

    @Override
    public FlowType.PlanningMode getPlanningMode() {
        return PLANNED;
    }


//    @Override
//    @Transactional
//    public FlowInstance create(String flowTypeId, Map<String, Object> scopes) {
//        FlowType ft = ftRepo.findById(flowTypeId)
//            .orElseThrow(() -> new FlowCreationException("Unknown FlowType"));
//
//
//        // Create FlowInstance
//        FlowInstance fi = new FlowInstance();
//        fiRepo.save(fi);
//        // Create root ScopeInstance
//        siSvc.createForFlow(fi.getId(), scopes);
//        return fi;
//    }

    @Override
    public FlowInstance create(FlowType flowType, Map<String, Object> scopeValues) {
        validateFlowUserAccess(flowType.getId());
        scopeValidationService.validate(flowType, scopeValues);
        // For PLANNED flows, the instance is created with a PLANNED status.
        // It represents a future task.
        FlowInstance instance = new FlowInstance();
        instance.setFlowType(flowType);
        // Snapshotting metadata as discussed in the evolution strategy document
        instance.setFlowScopeDefinitionSnapshot(flowType.getScopes());
        instance.setStageDefinitionSnapshot(flowType.getStages());
        instance.setStatus(FlowStatus.PLANNED);

        // Validate and create the ScopeInstance
        ScopeInstance scopeInstance = createAndValidateScope(instance, flowType.getScopes(), scopeValues);
        instance.setContext(scopeInstance); // Set bidirectional relationship

        return instance; // The repository will save this due to the @Transactional context
    }

    private ScopeInstance createAndValidateScope(FlowInstance flowInstance, DimensionalContext definition,
                                                 Map<String, Object> scopeValues) {
        // Create the ScopeInstance entity
        ScopeInstance scopeInstance = new ScopeInstance();
        scopeInstance.setFlowInstance(flowInstance);

        // Populate common dimensions from the map for easier querying
        scopeInstance.setOrgUnitId((String) scopeValues.get("orgUnitId"));
        scopeInstance.setScopeDate((LocalDate) scopeValues.get("scopeDate"));
        // Team is optional
        if (scopeValues.containsKey("teamId")) {
            scopeInstance.setTeamId((String) scopeValues.get("teamId"));
        }

        // Convert the entire map to a JSONB field for flexibility and custom dimensions
        scopeInstance.setScopeData(objectMapper.valueToTree(scopeValues));
        return scopeInstance;
    }

    void validateFlowUserAccess(String flowTypeId) {
        final var current = SecurityUtils.getCurrentUserDetails();
        if (current.isEmpty() || !current.get().isSuper() ||
            !current.get().getUserActiveFlowIds().contains(flowTypeId))
            throw new FlowCreationException("User not allowed this flow");
    }
}
