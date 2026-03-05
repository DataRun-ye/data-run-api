package org.nmcpye.datarun.web.rest.v1.formtemplate;

import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.common.ApiVersion;
import org.nmcpye.datarun.web.rest.legacy.JpaBaseResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.v1.formtemplate.FormTemplateResource.V1;

/**
 * REST controller for managing {@link DataTemplate}.
 */
@RestController
@RequestMapping(value = { V1 })
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class FormTemplateResource extends JpaBaseResource<DataTemplate> {
    protected static final String NAME = "/formTemplates";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    protected FormTemplateResource(DataTemplateService service,
            DataTemplateRepository repository) {
        super(service, repository);
    }

    @Override
    protected String getName() {
        return "formTemplates";
    }
}
