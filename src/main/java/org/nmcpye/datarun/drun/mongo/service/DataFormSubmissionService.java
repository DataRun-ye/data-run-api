package org.nmcpye.datarun.drun.mongo.service;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface DataFormSubmissionService
    extends IdentifiableMongoService<DataFormSubmission> {

    DataFormSubmission saveVersioning(DataFormSubmission submission);

    Page<DataFormSubmission> findAllByForm(List<String> forms, Pageable pageable);

    List<String> getTeamsAfterDate(Date createdDate);
}
