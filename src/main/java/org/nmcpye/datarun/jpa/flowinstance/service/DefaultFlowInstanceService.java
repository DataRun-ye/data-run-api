package org.nmcpye.datarun.jpa.flowinstance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator.FlowInstanceCreationStrategy;
import org.nmcpye.datarun.jpa.flowinstance.repository.FlowInstanceRepository;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeElement;
import org.nmcpye.datarun.jpa.scopeinstance.service.ScopeInstanceService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada 24/04/2025 <7amza.it@gmail.com>
 */
@Service
@Primary
@Transactional
public class DefaultFlowInstanceService
    extends DefaultJpaSoftDeleteService<FlowInstance>
    implements FlowInstanceService {

    private final FlowInstanceRepository repository;
    private final FlowTypeRepository flowTypeRepository;
    private final ScopeInstanceService scopeSvc;
    private final Map<FlowType.PlanningMode, FlowInstanceCreationStrategy> creationStrategies;
    private final ObjectMapper objectMapper; // For JSON handling

    public DefaultFlowInstanceService(FlowInstanceRepository repository,
                                      UserAccessService userAccessService,
                                      CacheManager cacheManager,
                                      List<FlowInstanceCreationStrategy> strategies,
                                      FlowTypeRepository flowTypeRepository,
                                      ScopeInstanceService scopeSvc, ObjectMapper objectMapper) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.flowTypeRepository = flowTypeRepository;
        this.scopeSvc = scopeSvc;
        this.creationStrategies = strategies.stream()
            .collect(Collectors.toMap(FlowInstanceCreationStrategy::getPlanningMode, Function.identity()));
        this.objectMapper = objectMapper;
    }


    @Transactional
    @Override
    public FlowInstance createFlowInstance(String flowTypeId,
                                           Map<String, Object> scopeValues) {

        // 1. Load the latest active version of the FlowType.
        // In a real implementation, you might pass a specific version or have more complex lookup logic.
        FlowType flowType = flowTypeRepository.findById(flowTypeId)
            .orElseThrow(() -> new EntityNotFoundException("Active FlowType with id " + flowTypeId + " not found."));

        // 2. Select the correct creation strategy based on the FlowType's planning mode.
        FlowInstanceCreationStrategy strategy = creationStrategies.get(flowType.getPlanningMode());
        if (strategy == null) {
            throw new UnsupportedOperationException("No creation strategy found for planning mode: " + flowType.getPlanningMode());
        }

        // 3. Delegate the creation process to the selected strategy.
        return strategy.create(flowType, scopeValues);


        FlowType ft = flowTypeRepository.findById(flowTypeId)
            .orElseThrow(() -> new IllegalArgumentException("FlowType not found"));
        if (ft.getPlanningMode() != FlowType.PlanningMode.PLANNED) {
            throw new IllegalStateException("FlowType is not PLANNED");
        }
        // Validate scopes
        var defs = ft.getScopes().getScopeElements();
        for (ScopeElement def : defs) {
            if (def.isRequired() && !scopes.containsKey(def.getKey())) {
                throw new IllegalArgumentException("Missing scope: " + def.getKey());
            }
        }
        // Create instance
        FlowInstance fi = new FlowInstance();
        fi.setFlowType(ft);
        fi.setScopes(convertScopes(scopes));
        fi.setStatus(FlowStatus.IN_PROGRESS);
        fi = repository.save(fi);

        // Persist scope context
        scopeSvc.createForFlow(fi.getId(), scopes);
        return fi;
    }


    private Map<String, String> convertScopes(Map<String, Object> raw) {
        var map = new HashMap<String, String>();
        raw.forEach((k, v) -> map.put(k, v.toString()));
        return map;
    }
}
