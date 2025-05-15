package org.nmcpye.datarun.web.rest.mongo.assignmenthistory;

import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.repository.AssignmentSubmissionHistoryRepository;
import org.nmcpye.datarun.mongo.service.AssignmentSubmissionHistoryService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.nmcpye.datarun.web.rest.mongo.submission.GenericQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.mongo.assignmenthistory.AssignmentSubmissionHistoryResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.mongo.assignmenthistory.AssignmentSubmissionHistoryResource.V1;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class AssignmentSubmissionHistoryResource
    extends MongoBaseResource<AssignmentSubmissionHistory> {
    protected static final String NAME = "/assignments/history";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;
    final private GenericQueryService queryService;

    public AssignmentSubmissionHistoryResource(AssignmentSubmissionHistoryService dataFormSubmissionService,
                                               AssignmentSubmissionHistoryRepository
                                                         dataFormSubmissionRepositoryCustom,
                                               GenericQueryService queryService) {
        super(dataFormSubmissionService, dataFormSubmissionRepositoryCustom);
        this.queryService = queryService;
    }

    @Override
    protected String getName() {
        return "history";
    }
}
