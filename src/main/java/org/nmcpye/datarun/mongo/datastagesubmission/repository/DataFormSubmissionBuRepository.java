package org.nmcpye.datarun.mongo.datastagesubmission.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.mongo.common.repository.MongoAuditableRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the DataFormSubmissionBu entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataFormSubmissionBuRepository
    extends MongoAuditableRepository<DataFormSubmissionBu> {
}
