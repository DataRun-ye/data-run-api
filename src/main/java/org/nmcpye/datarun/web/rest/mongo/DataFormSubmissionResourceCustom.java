package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionServiceCustom;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/dataSubmissions")
public class DataFormSubmissionResourceCustom extends AbstractMongoResource<DataFormSubmission> {

    public DataFormSubmissionResourceCustom(DataFormSubmissionServiceCustom dataFormSubmissionServiceCustom,
                                            DataFormSubmissionRepositoryCustom dataFormSubmissionRepositoryCustom) {
        super(dataFormSubmissionServiceCustom, dataFormSubmissionRepositoryCustom);
    }

    @Override
    protected String getName() {
        return "dataSubmissions";
    }
}
