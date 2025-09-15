package org.nmcpye.datarun.mongo.datastagesubmission.repository;

import org.nmcpye.datarun.mongo.common.repository.MongoIdentifiableRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataFormSubmission entity.
 */
@Repository
//@JaversSpringDataAuditable
public interface DataFormSubmissionRepository
    extends MongoIdentifiableRepository<DataFormSubmission> {
    public interface FormVersionPair {
        String getForm();
        Integer getCurrentVersion();
    }

    // Aggregation to return distinct (form, currentVersion) pairs.
    @Aggregation(pipeline = {
        "{ $group: { _id: { form: '$form', currentVersion: '$currentVersion' } } }",
        "{ $project: { _id: 0, form: '$_id.form', currentVersion: '$_id.currentVersion' } }"
    })
    List<FormVersionPair> findDistinctFormAndVersion();

    @Query("{ 'serialNumber' : { $exists: false } }")
    List<DataFormSubmission> findBySerialNumberNull();

    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmission> findAllByUser(Pageable pageable);
}
