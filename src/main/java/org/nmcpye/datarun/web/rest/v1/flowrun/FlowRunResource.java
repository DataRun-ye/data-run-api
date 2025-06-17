package org.nmcpye.datarun.web.rest.v1.flowrun;

import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.repository.FlowInstanceRepository;
import org.nmcpye.datarun.jpa.flowinstance.service.FlowInstanceScopeValidator;
import org.nmcpye.datarun.jpa.flowinstance.service.FlowInstanceService;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST Extended controller for managing {@link FlowInstance}.
 */
@RestController
@RequestMapping(value = {
    FlowRunResource.CUSTOM,
    FlowRunResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class FlowRunResource extends JpaBaseResource<FlowInstance> {
    protected static final String NAME = "/flowRuns";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final FlowInstanceService service;
    private final FlowInstanceRepository repository;
    private final FlowTypeRepository flowTypeRepository;
    private final FlowInstanceScopeValidator flowInstanceScopeValidator;

    public FlowRunResource(FlowInstanceService service, FlowInstanceRepository repository,
                           FlowTypeRepository flowTypeRepository, FlowInstanceScopeValidator flowInstanceScopeValidator) {
        super(service, repository);
        this.service = service;
        this.repository = repository;
        this.flowTypeRepository = flowTypeRepository;
        this.flowInstanceScopeValidator = flowInstanceScopeValidator;
    }

    @Override
    protected String getName() {
        return "flowRuns";
    }

//    @PostMapping("/flowRuns")
//    public ResponseEntity<?> createFlowInstance(
//        @Valid @RequestBody FlowRunRequest request) {
//
//        FlowType flowType = flowTypeRepository.findByUid(request.getFlowTypeUid())
//            .orElse(null);
//
//        FlowRun flowRun = new FlowRun();
//        flowRun.setFlowType(flowType);
//        flowRun.setScopes(request.getScopes());
//
//        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
//        final var result = fLowRunScopeValidator.validateScopes(flowRun);
//        if (!result.isValid()) {
//            summary.getFailed().put(flowRun.getUid(), result.toString());
//            return ResponseEntity.badRequest()
//                .body(summary);
//        }
//
//        saveEntity(flowRun, summary);
//
//        return ResponseEntity.ok(summary);
//    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FlowInstance> updateEntity(String uid, FlowInstance entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(FlowInstance entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(FlowInstance entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<FlowInstance> entities) {
        return super.saveAll(entities);
    }
}
