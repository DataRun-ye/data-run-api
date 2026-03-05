package org.nmcpye.datarun.web.rest.v1.formtemplate;

import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.common.ApiVersion;
import org.nmcpye.datarun.datatemplateprocessor.FormTemplateProcessor;
import org.nmcpye.datarun.web.rest.legacy.JpaBaseResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link TemplateVersion}.
 */
@RestController
@RequestMapping(value = { TemplateVersionResource.V1 })
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class TemplateVersionResource extends JpaBaseResource<TemplateVersion> {
    protected static final String NAME = "/formTemplateVersions";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    protected final FormTemplateProcessor formTemplateProcessor;

    protected TemplateVersionResource(TemplateVersionService service,
            TemplateVersionRepository repository,
            FormTemplateProcessor formTemplateProcessor) {
        super(service, repository);
        this.formTemplateProcessor = formTemplateProcessor;
    }

    @Override
    protected void saveEntity(TemplateVersion payLoadEntity, EntitySaveSummaryVM summary) {
        // super.saveEntity(payLoadEntity, summary);
    }

    @Override
    protected String getName() {
        return "formTemplateVersions";
    }
}
