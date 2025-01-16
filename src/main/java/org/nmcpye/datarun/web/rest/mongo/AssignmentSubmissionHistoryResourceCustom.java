package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.repository.AssignmentSubmissionHistoryRepository;
import org.nmcpye.datarun.mongo.service.AssignmentSubmissionHistoryService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.mongo.submission.GenericQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/assignments/history")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class AssignmentSubmissionHistoryResourceCustom
    extends AbstractMongoResource<AssignmentSubmissionHistory> {

    final private GenericQueryService queryService;

    public AssignmentSubmissionHistoryResourceCustom(AssignmentSubmissionHistoryService dataFormSubmissionService,
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
