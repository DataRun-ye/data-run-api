package org.nmcpye.datarun.web.rest.mongo.dataformtemplate;

import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.mongo.dataformtemplate.DataFormTemplateResource.CUSTOM;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping(value = {CUSTOM})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormTemplateResource extends MongoBaseResource<DataFormTemplate> {
    protected static final String NAME = "/dataFormTemplates";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;

    private final FormTemplateProcessor formTemplateProcessor;

    public DataFormTemplateResource(DataFormTemplateService templateService,
                                    DataFormTemplateRepository dataFormRepository,
                                    FormTemplateProcessor formTemplateProcessor) {
        super(templateService, dataFormRepository);
        this.formTemplateProcessor = formTemplateProcessor;
    }

    @Override
    protected String getName() {
        return "dataFormTemplates";
    }

    @Override
    protected DataFormTemplate preProcess(DataFormTemplate entity) {
        return (DataFormTemplate) formTemplateProcessor
            .processMetadata(formTemplateProcessor.validate(entity));
    }
}
