package org.nmcpye.datarun.web.rest.postgres.optionset;

import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.drun.postgres.repository.OptionSetRepository;
import org.nmcpye.datarun.drun.postgres.service.OptionSetService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.postgres.AbstractJpaResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/optionSets")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OptionSetResource
    extends AbstractJpaResource<OptionSet> {

    public OptionSetResource(OptionSetService metadataSubmissionService,
                             OptionSetRepository metadataSchemaRepository) {
        super(metadataSubmissionService, metadataSchemaRepository);
    }

    @Override
    protected String getName() {
        return "optionSets";
    }
}
