package org.nmcpye.datarun.web.rest.v1.datastagedefinition;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.datastage.DataStageDefinition;
import org.nmcpye.datarun.jpa.datastage.repository.DataStageDefinitionRepository;
import org.nmcpye.datarun.jpa.datastage.service.DataStageDefinitionService;
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
 * REST controller for managing {@link DataStageDefinition}.
 */
@RestController
@RequestMapping(value = {DataStageDefinitionResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class DataStageDefinitionResource extends JpaBaseResource<DataStageDefinition> {
    protected static final String NAME = "/dataStageDefinitions";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final DataStageDefinitionService stageDefinitionService;

    protected DataStageDefinitionResource(DataStageDefinitionService service,
                                          DataStageDefinitionRepository repository) {
        super(service, repository);
        this.stageDefinitionService = service;
    }

    @Override
    protected String getName() {
        return "dataStageDefinitions";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataStageDefinition> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataStageDefinition entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(DataStageDefinition entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }
}
