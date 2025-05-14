package org.nmcpye.datarun.web.rest.v1.formtemplate;

import jakarta.validation.Valid;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.repository.FormTemplateRepository;
import org.nmcpye.datarun.mongo.service.FormTemplateService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

import static org.nmcpye.datarun.web.rest.v1.formtemplate.FormTemplateResource.V1;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping(value = {V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class FormTemplateResource extends MongoBaseResource<FormTemplate> {
    protected static final String NAME = "/dataFormTemplates";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final FormTemplateProcessor formTemplateProcessor;
    private final FormTemplateService versionService;

    //
    public FormTemplateResource(FormTemplateService templateService,
                                FormTemplateRepository dataFormRepository,
                                FormTemplateProcessor formTemplateProcessor,
                                FormTemplateService versionService) {
        super(templateService, dataFormRepository);
        this.formTemplateProcessor = formTemplateProcessor;
        this.versionService = versionService;
    }

    @Override
    protected void applySecurityConstraints(Query query) {
        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        query.addCriteria(Criteria.where("uid").in(user.getUserFormsUIDs()));
    }

    @Override
    protected String getName() {
        return "dataFormTemplates";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<FormTemplate> templates) {
        return super.saveAll(templates);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(FormTemplate formTemplate) {
        return super.saveOne(formTemplate);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(FormTemplate entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<FormTemplate> updateEntity(String uid, FormTemplate entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }
}
