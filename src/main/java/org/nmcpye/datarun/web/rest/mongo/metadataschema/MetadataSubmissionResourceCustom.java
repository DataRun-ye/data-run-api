package org.nmcpye.datarun.web.rest.mongo.metadataschema;

import org.nmcpye.datarun.mongo.domain.MetadataSubmission;
import org.nmcpye.datarun.mongo.metadataschema.repository.MetadataSubmissionGranularRepository;
import org.nmcpye.datarun.mongo.metadataschema.repository.MetadataSubmissionRepository;
import org.nmcpye.datarun.mongo.metadataschema.service.MetadataSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.mongo.metadataschema.MetadataSubmissionResourceCustom.CUSTOM;
import static org.nmcpye.datarun.web.rest.mongo.metadataschema.MetadataSubmissionResourceCustom.V1;

/**
 * REST controller for managing {@link MetadataSubmission}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class MetadataSubmissionResourceCustom
    extends MongoBaseResource<MetadataSubmission> {
    protected static final String NAME = "/metadataSubmissions";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final MetadataSubmissionGranularRepository metadataSubmissionGranularRepository;

    public MetadataSubmissionResourceCustom(MetadataSubmissionService metadataSubmissionService,
                                            MetadataSubmissionRepository metadataSubmissionRepository, MetadataSubmissionGranularRepository metadataSubmissionGranularRepository) {
        super(metadataSubmissionService, metadataSubmissionRepository);
        this.metadataSubmissionGranularRepository = metadataSubmissionGranularRepository;
    }

    @Override
    protected Page<MetadataSubmission> getList(QueryRequest queryRequest, String jsonQueryBody) {
        return metadataSubmissionGranularRepository.getReferencedMetadataSubmissions(queryRequest);
    }

    @Override
    protected String getName() {
        return "metadataSubmissions";
    }
}
