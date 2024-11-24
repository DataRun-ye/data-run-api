package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.domain.MetadataSchema;
import org.nmcpye.datarun.drun.mongo.repository.MetadataSchemaRepository;
import org.nmcpye.datarun.drun.mongo.repository.MetadataSubmissionGranularRepository;
import org.nmcpye.datarun.drun.mongo.service.MetadataSchemaService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/metadataSchemas")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class MetadataSchemaResource
    extends AbstractMongoResource<MetadataSchema> {

    public MetadataSchemaResource(MetadataSchemaService metadataSubmissionService,
                                  MetadataSchemaRepository metadataSchemaRepository,
                                  MetadataSubmissionGranularRepository metadataSubmissionGranularRepository) {
        super(metadataSubmissionService, metadataSchemaRepository);
    }

    @Override
    protected String getName() {
        return "metadataSchemas";
    }
}
