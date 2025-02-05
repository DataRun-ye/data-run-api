package org.nmcpye.datarun.web.rest.mongo.metadataschema;

import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.MetadataSubmission;
import org.nmcpye.datarun.mongo.repository.MetadataSubmissionGranularRepository;
import org.nmcpye.datarun.mongo.repository.MetadataSubmissionRepositoryCustom;
import org.nmcpye.datarun.mongo.service.MetadataSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.mongo.AbstractMongoResource;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/metadataSubmissions")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class MetadataSubmissionResourceCustom
    extends AbstractMongoResource<MetadataSubmission> {

    private final MetadataSubmissionGranularRepository metadataSubmissionGranularRepository;

    public MetadataSubmissionResourceCustom(MetadataSubmissionService metadataSubmissionService,
                                            MetadataSubmissionRepositoryCustom metadataSubmissionRepositoryCustom, MetadataSubmissionGranularRepository metadataSubmissionGranularRepository) {
        super(metadataSubmissionService, metadataSubmissionRepositoryCustom);
        this.metadataSubmissionGranularRepository = metadataSubmissionGranularRepository;
    }

    @Override
    protected Page<MetadataSubmission> getList(Pageable pageable, QueryRequest queryRequest) {
        return metadataSubmissionGranularRepository.getReferencedMetadataSubmissions(pageable, queryRequest);
    }

    @Override
    protected String getName() {
        return "metadataSubmissions";
    }
}
