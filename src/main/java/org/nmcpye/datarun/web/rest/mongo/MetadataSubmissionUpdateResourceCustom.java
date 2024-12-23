package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.MetadataSubmissionUpdate;
import org.nmcpye.datarun.mongo.repository.MetadataSubmissionGranularRepository;
import org.nmcpye.datarun.mongo.repository.MetadataSubmissionUpdateRepositoryCustom;
import org.nmcpye.datarun.mongo.service.MetadataSubmissionUpdateService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/metadataSubmissionUpdates")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class MetadataSubmissionUpdateResourceCustom
    extends AbstractMongoResource<MetadataSubmissionUpdate> {

    private final MetadataSubmissionGranularRepository metadataSubmissionGranularRepository;

    public MetadataSubmissionUpdateResourceCustom(MetadataSubmissionUpdateService metadataSubmissionService,
                                                  MetadataSubmissionUpdateRepositoryCustom metadataSubmissionRepositoryCustom, MetadataSubmissionGranularRepository metadataSubmissionGranularRepository) {
        super(metadataSubmissionService, metadataSubmissionRepositoryCustom);
        this.metadataSubmissionGranularRepository = metadataSubmissionGranularRepository;
    }

//    @Override
//    protected Page<MetadataSubmission> getList(Pageable pageable, QueryRequest queryRequest) {
//        return metadataSubmissionGranularRepository.getReferencedMetadataSubmissions(pageable, queryRequest);
//    }

    @Override
    protected String getName() {
        return "metadataSubmissionUpdates";
    }
}
