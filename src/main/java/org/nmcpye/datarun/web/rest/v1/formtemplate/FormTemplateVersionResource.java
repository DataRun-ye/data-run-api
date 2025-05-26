package org.nmcpye.datarun.web.rest.v1.formtemplate;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.repository.FormTemplateVersionRepository;
import org.nmcpye.datarun.mongo.service.FormTemplateVersionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.nmcpye.datarun.web.rest.v1.formtemplate.FormTemplateVersionResource.V1;

/**
 * REST controller for managing {@link FormTemplateVersion}.
 */
@RestController
@RequestMapping(value = {V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class FormTemplateVersionResource extends MongoBaseResource<FormTemplateVersion> {
    protected static final String NAME = "/formTemplateVersions";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final FormTemplateVersionService templateService;
    protected final FormTemplateProcessor formTemplateProcessor;

    protected FormTemplateVersionResource(FormTemplateVersionService service,
                                          FormTemplateVersionRepository repository,
                                          FormTemplateProcessor formTemplateProcessor) {
        super(service, repository);
        this.templateService = service;
        this.formTemplateProcessor = formTemplateProcessor;
    }

    @Override
    protected String getName() {
        return "formTemplateVersions";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid List<FormTemplateVersion> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(@Valid FormTemplateVersion entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(@Valid FormTemplateVersion entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }

    @Override
    public ResponseEntity<FormTemplateVersion> getById(String id) {
        return super.getById(id);
    }
}
