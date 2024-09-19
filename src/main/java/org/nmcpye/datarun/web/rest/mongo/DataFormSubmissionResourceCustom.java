package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing {@link org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/dataSubmissions")
public class DataFormSubmissionResourceCustom
    extends AbstractMongoResource<DataFormSubmission> {

    public DataFormSubmissionResourceCustom(DataFormSubmissionService dataFormSubmissionService,
                                            DataFormSubmissionRepositoryCustom dataFormSubmissionRepositoryCustom) {
        super(dataFormSubmissionService, dataFormSubmissionRepositoryCustom);
    }

    @GetMapping("/form")
    public ResponseEntity<PagedResponse<DataFormSubmission>> getByForm(@RequestParam(name = "filter", required = false) String filter, @ParameterObject Pageable pageable,
                                                                       @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging) {

        if (!paging && filter != null) {
            pageable = Pageable.unpaged();
            Page<DataFormSubmission> submissionsPage = ((DataFormSubmissionService) identifiableService).findAllByForm(List.of(filter), pageable);
            PagedResponse<DataFormSubmission> response = initPageResponse(paging, submissionsPage);
            return ResponseEntity.ok(response);
        }

        Page<DataFormSubmission> submissionsPage = identifiableService.findAllByUser(pageable);

        PagedResponse<DataFormSubmission> response = initPageResponse(paging, submissionsPage);
        return ResponseEntity.ok(response);
    }


    @Override
    protected String getName() {
        return "dataSubmissions";
    }
}
