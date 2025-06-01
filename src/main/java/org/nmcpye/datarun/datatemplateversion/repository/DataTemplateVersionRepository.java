package org.nmcpye.datarun.datatemplateversion.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.common.mongo.repository.MongoAuditableRepository;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.datatemplateversion.DataTemplateTemplateVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataTemplateVersionRepository
    extends MongoAuditableRepository<DataTemplateTemplateVersion> {
    // Returns the single FormInstance with highest version for this template
    Optional<DataTemplateTemplateVersion> findTopByTemplateUidOrderByVersionNumberDesc(String templateId);

    // Returns a specific version, if it exists
    Optional<DataTemplateTemplateVersion> findByTemplateUidAndVersionNumber(String templateId, int version);


    // List all versions sorted descending
    Page<DataTemplateTemplateVersion> findAllByTemplateUidOrderByVersionNumberDesc(String templateId, Pageable pageable);

    Page<DataTemplateTemplateVersion> findAllByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids, Pageable pageable);


    Page<DataTemplateTemplateVersion> findTopByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids, Pageable pageable);

    Set<DataTemplateTemplateVersion> findAllByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

    List<DataTemplateTemplateVersion> findTopByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

    @Query(value = "{ 'fields.type': { $in: ?0 }}")
    List<DataTemplateTemplateVersion> findByFieldType(List<ValueType> types);
}
