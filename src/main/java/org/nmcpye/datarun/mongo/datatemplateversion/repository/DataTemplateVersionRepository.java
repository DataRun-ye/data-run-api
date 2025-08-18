package org.nmcpye.datarun.mongo.datatemplateversion.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.mongo.common.repository.MongoIdentifiableRepository;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
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
    extends MongoIdentifiableRepository<DataTemplateVersion> {
    // Returns the single FormInstance with highest version for this template
    Optional<DataTemplateVersion> findTopByTemplateUidOrderByVersionNumberDesc(String templateId);

    // Returns a specific version, if it exists
    Optional<DataTemplateVersion> findByTemplateUidAndVersionNumber(@NotNull @Size(max = 11) String templateId, int version);

    Optional<DataTemplateVersion> findByTemplateUidAndUid(@NotNull @Size(max = 11) String templateUid, String id);


    // List all versions sorted descending
    Page<DataTemplateVersion> findAllByTemplateUidOrderByVersionNumberDesc(String templateId, Pageable pageable);

    Page<DataTemplateVersion> findAllByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids, Pageable pageable);

    Set<DataTemplateVersion> findAllByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

    List<DataTemplateVersion> findDistinctByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

//    @Query(value = "{ 'templateUid': { $in: ?0 }, 'fields.type': { $in: ?1 } }", fields = "{ 'fields.optionSet': 1, '_id': 0 }")
//    List<String> findByFormsAndPermission(Set<String> templateUid, Set<ValueType> types);

    @Query(value = "{ 'fields.type': { $in: ?0 }}")
    List<DataTemplateVersion> findByFieldType(List<ValueType> types);
}
