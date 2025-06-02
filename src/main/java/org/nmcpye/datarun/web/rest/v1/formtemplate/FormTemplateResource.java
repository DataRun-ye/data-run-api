package org.nmcpye.datarun.web.rest.v1.formtemplate;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.nmcpye.datarun.web.rest.v1.formtemplate.FormTemplateResource.V1;

/**
 * REST controller for managing {@link DataTemplate}.
 */
@RestController
@RequestMapping(value = {V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class FormTemplateResource extends JpaBaseResource<DataTemplate> {
    protected static final String NAME = "/formTemplates";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final DataTemplateService templateService;

    protected FormTemplateResource(DataTemplateService service,
                                   DataTemplateRepository repository,
                                   FormTemplateProcessor formTemplateProcessor) {
        super(service, repository);
        this.templateService = service;
    }

    @Override
    protected String getName() {
        return "formTemplates";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataTemplate> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataTemplate entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(DataTemplate entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }
}
