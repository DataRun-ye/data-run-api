package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.common.mongo.repository.MongoAuditableRepository;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplateVersion;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataFormTemplateVersionRepository
    extends MongoAuditableRepository<DataFormTemplateVersion> {
    // Returns the single FormInstance with highest version for this template
    Optional<DataFormTemplateVersion> findTopByTemplateUidOrderByVersionDesc(String templateId);

    // Returns a specific version, if it exists
    Optional<DataFormTemplateVersion> findByTemplateUidAndVersion(String templateId, int version);

    // List all versions sorted descending
    List<DataFormTemplateVersion> findAllByTemplateUidOrderByVersionDesc(String templateId);

    @Query(value = "{ 'fields.type': { $in: ?0 }}")
    List<DataFormTemplateVersion> findByFieldType(List<ValueType> types);
}
