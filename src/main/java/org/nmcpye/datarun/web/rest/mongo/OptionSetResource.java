package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.OptionSet;
import org.nmcpye.datarun.mongo.repository.OptionSetRepository;
import org.nmcpye.datarun.mongo.service.OptionSetService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
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
    extends AbstractMongoResource<OptionSet> {

    public OptionSetResource(OptionSetService metadataSubmissionService,
                             OptionSetRepository metadataSchemaRepository) {
        super(metadataSubmissionService, metadataSchemaRepository);
    }

    @Override
    protected String getName() {
        return "optionSets";
    }
}
