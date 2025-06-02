package org.nmcpye.datarun.mongo.domain.dataform;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.mongo.common.repository.MongoAuditableRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface FormTemplateLegacyRepository
    extends MongoAuditableRepository<FormTemplateLegacy> {
}
