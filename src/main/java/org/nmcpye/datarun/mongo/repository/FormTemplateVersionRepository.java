package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface FormTemplateVersionRepository
    extends MongoRepository<FormTemplateVersion, String> {
    @Query("{'uid': ?0}")
    Optional<FormTemplateVersion> findByUid(String uid);

    List<FormTemplateVersion> findAllByUidIn(Collection<String> uids);
    // Returns the single FormInstance with highest version for this template
    Optional<FormTemplateVersion> findTopByTemplateUidOrderByVersionNumberDesc(String templateId);

    // Returns a specific version, if it exists
    Optional<FormTemplateVersion> findByTemplateUidAndVersionNumber(String templateId, int version);

    // List all versions sorted descending
    Page<FormTemplateVersion> findAllByTemplateUidOrderByVersionNumberDesc(String templateId, Pageable pageable);

    @Query(value = "{ 'fields.type': { $in: ?0 }}")
    List<FormTemplateVersion> findByFieldType(List<ValueType> types);
}
