package org.nmcpye.datarun.web.rest.v1.formtemplate;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.mongo.datatemplateversion.service.DateTemplateVersionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing {@link DataTemplateVersion}.
 */
@RestController
@RequestMapping(value = {FormTemplateVersionResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class FormTemplateVersionResource extends MongoBaseResource<DataTemplateVersion> {
    protected static final String NAME = "/formTemplateVersions";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final DateTemplateVersionService templateService;
    protected final FormTemplateProcessor formTemplateProcessor;

    protected FormTemplateVersionResource(DateTemplateVersionService service,
                                          DataTemplateVersionRepository repository,
                                          FormTemplateProcessor formTemplateProcessor) {
        super(service, repository);
        this.templateService = service;
        this.formTemplateProcessor = formTemplateProcessor;
    }

    @Override
    protected String getName() {
        return "formTemplateVersions";
    }

    @Override
    protected List<DataTemplateVersion> preProcess(List<DataTemplateVersion> payLoadEntities) {
        return payLoadEntities.stream().map(e ->
            (DataTemplateVersion) formTemplateProcessor
                .processMetadata(formTemplateProcessor.validate(e))).toList();
    }
}
