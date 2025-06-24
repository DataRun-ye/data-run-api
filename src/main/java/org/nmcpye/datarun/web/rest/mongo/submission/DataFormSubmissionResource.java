package org.nmcpye.datarun.web.rest.mongo.submission;

import org.nmcpye.datarun.jpa.team.service.TeamService;
import org.nmcpye.datarun.mongo.datastagesubmission.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.datastagesubmission.service.DataFormSubmissionService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping(value = {DataFormSubmissionResource.CUSTOM, DataFormSubmissionResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormSubmissionResource
    extends MongoBaseResource<DataFormSubmission> {
    protected static final String NAME = "/dataSubmission";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    final private DataFormSubmissionService dataFormSubmissionService;
    final TeamService teamService;

    public DataFormSubmissionResource(DataFormSubmissionService dataFormSubmissionService,
                                      DataFormSubmissionRepository
                                          dataFormSubmissionRepository,
                                      TeamService teamService) {
        super(dataFormSubmissionService, dataFormSubmissionRepository);
        this.dataFormSubmissionService = dataFormSubmissionService;
        this.teamService = teamService;
    }

    @Deprecated(since = "V7, main method do the same now")
    @RequestMapping(value = "objects", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<PagedResponse<?>> getObjectSubmissions(QueryRequest queryRequest,
                                                                 @RequestBody(required = false) String jsonQuery) {
        QueryRequestValidator.validate(queryRequest);
        Page<DataFormSubmission> resultPage = getList(queryRequest, jsonQuery);

        String next = createNextPageLink(resultPage);

        PagedResponse<DataFormSubmission> response = new
            PagedResponse<>(resultPage, getName(), next);
        return ResponseEntity.ok(response);
    }

    @Override
    protected DataFormSubmission postProcess(DataFormSubmission submission,
                                             QueryRequest queryRequest,
                                             String jsonQuery) {
        if (queryRequest.isFlatten()) {
            Map<String, Object> formData = submission.getFormData();
            formData = FormSubmissionDataUtil.flatten(formData, false, true);
            submission.setFormData(formData);
        }

        return submission;
    }

    @Override
    protected String getName() {
        return "dataSubmission";
    }
}
