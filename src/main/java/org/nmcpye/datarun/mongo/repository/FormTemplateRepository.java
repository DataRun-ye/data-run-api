package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.common.mongo.repository.MongoAuditableRepository;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface FormTemplateRepository
    extends MongoAuditableRepository<FormTemplate> {
    Set<FormTemplate> findAllByUidInAndDisabledIsNot(Collection<String> uids, Boolean disabled);
}
