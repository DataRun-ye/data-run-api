package org.nmcpye.datarun.web.rest.v1.datastagedefinition;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;
import org.nmcpye.datarun.jpa.stagedefinition.repository.StageDefinitionRepository;
import org.nmcpye.datarun.jpa.stagedefinition.service.DataStageDefinitionService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * REST controller for managing {@link StageDefinition}.
 */
@RestController
@RequestMapping(value = {DataStageDefinitionResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class DataStageDefinitionResource extends JpaBaseResource<StageDefinition> {
    protected static final String NAME = "/dataStageDefinitions";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final DataStageDefinitionService stageDefinitionService;

    protected DataStageDefinitionResource(DataStageDefinitionService service,
                                          StageDefinitionRepository repository) {
        super(service, repository);
        this.stageDefinitionService = service;
    }

    @Override
    protected String getName() {
        return "dataStageDefinitions";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<StageDefinition> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(StageDefinition entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(StageDefinition entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }
}
