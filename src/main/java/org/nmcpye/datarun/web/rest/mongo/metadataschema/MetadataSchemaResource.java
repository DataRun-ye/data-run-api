package org.nmcpye.datarun.web.rest.mongo.metadataschema;

import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.MetadataSchema;
import org.nmcpye.datarun.mongo.metadataschema.repository.MetadataSchemaRepository;
import org.nmcpye.datarun.mongo.metadataschema.repository.MetadataSubmissionGranularRepository;
import org.nmcpye.datarun.mongo.metadataschema.service.MetadataSchemaService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.mongo.metadataschema.MetadataSchemaResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.mongo.metadataschema.MetadataSchemaResource.V1;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class MetadataSchemaResource
    extends MongoBaseResource<MetadataSchema> {
    protected static final String NAME = "/metadataSchemas";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

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
