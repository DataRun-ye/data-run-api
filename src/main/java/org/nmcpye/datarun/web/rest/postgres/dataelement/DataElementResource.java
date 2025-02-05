package org.nmcpye.datarun.web.rest.postgres.dataelement;

import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.repository.DataElementRepository;
import org.nmcpye.datarun.drun.postgres.service.DataElementService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.postgres.AbstractRelationalResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/dataElements")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataElementResource
    extends AbstractRelationalResource<DataElement> {

    public DataElementResource(DataElementService metadataSubmissionService,
                               DataElementRepository metadataSchemaRepository) {
        super(metadataSubmissionService, metadataSchemaRepository);
    }

    @Override
    protected String getName() {
        return "dataElements";
    }
}
