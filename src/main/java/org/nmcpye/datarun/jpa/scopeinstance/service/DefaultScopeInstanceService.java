package org.nmcpye.datarun.jpa.scopeinstance.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator.ScopeValidationService;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeInstance;
import org.nmcpye.datarun.jpa.scopeinstance.repository.ScopeInstanceRepository;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class DefaultScopeInstanceService
    extends DefaultJpaIdentifiableService<ScopeInstance>
    implements ScopeInstanceService {

    private final ScopeInstanceRepository repository;
    private final ScopeValidationService scopeValidationService;

    public DefaultScopeInstanceService(ScopeInstanceRepository repository,
                                       UserAccessService userAccessService,
                                       CacheManager cacheManager,
                                       ScopeValidationService scopeValidationService) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.scopeValidationService = scopeValidationService;
    }

//    @Transactional
//    public ScopeInstance createRootScope(FlowInstance flow) {
//        ScopeInstance scope = new ScopeInstance();
//        scope.setFlowInstance(flow);
//        scope.setScopeData(resolveRootScopes(flow));
//
//        try {
//            return scopeRepo.save(scope);
//        } catch (DataIntegrityViolationException ex) {
//            // Handle duplicate root creation
//            return scopeRepo.findByFlowInstance(flow)
//                .orElseThrow();
//        }
//    }

//    @Transactional
//    public StageSubmission createStageSubmission(StageRequest request) {
//        ScopeInstance scope = resolveScope(request);
//        scopeRepo.lock(scope.getId());  // SELECT FOR UPDATE
//
//        StageSubmission submission = new StageSubmission();
//        submission.setScopeInstance(scope);
//        // ... set other fields
//
//        return stageRepo.save(submission);
//    }

    /// -------- old
    @Transactional
    @Override
    public ScopeInstance createForFlow(String flowInstanceId, Map<String, Object> scopeElements) {
        ScopeInstance si = new ScopeInstance();
        si.setFlowInstance(new FlowInstance(flowInstanceId));
        si.setScopeElements(scopeElements);

        return repository.save(si);
    }

    @Transactional
    @Override
    public ScopeInstance createForStage(String flowInstanceId, String stageSubmissionId,
                                        Map<String, String> scopeElements, String entityInstanceId) {
        ScopeInstance si = new ScopeInstance();
        si.setFlowInstance(new FlowInstance(flowInstanceId));
        si.setStageSubmission(new StageInstance(stageSubmissionId));
        si.setScopeElements(scopeElements);
        si.setEntityInstance(new EntityInstance(entityInstanceId));
        return repository.save(si);
    }
}
