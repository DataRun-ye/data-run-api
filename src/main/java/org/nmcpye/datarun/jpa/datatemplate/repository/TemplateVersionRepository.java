package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;

/// Spring Data jpa repository for the DataTemplate entity.
@Repository
//@JaversSpringDataAuditable
public interface TemplateVersionRepository
    extends JpaIdentifiableRepository<TemplateVersion> {
    String TEMPLATE_UID_VERSION_NO_JPA_CACHE = "templateUidVersionNoCache";
    String TEMPLATE_UID_VERSION_UID_JPA_CACHE = "templateUidVersionUidCache";
    String TEMPLATE_UID_LATEST_VERSION_JPA_CACHE = "templateUidLatestVersionCache";

    List<TemplateVersion> findAllByTemplateUidIn(Collection<String> templateUids);


    // Returns the single FormInstance with highest version for this template
    @Cacheable(cacheNames = TEMPLATE_UID_LATEST_VERSION_JPA_CACHE)
    Optional<TemplateVersion> findTopByTemplateUidOrderByVersionNumberDesc(String templateId);

    // Returns a specific version, if it exists
    @Cacheable(cacheNames = TEMPLATE_UID_VERSION_NO_JPA_CACHE)
    Optional<TemplateVersion> findByTemplateUidAndVersionNumber(@NotNull @Size(max = 11) String templateId, int version);

    @Cacheable(cacheNames = TEMPLATE_UID_VERSION_UID_JPA_CACHE)
    Optional<TemplateVersion> findByTemplateUidAndUid(@NotNull @Size(max = 11) String templateUid, String id);


    // List all versions sorted descending
    Page<TemplateVersion> findAllByTemplateUidOrderByVersionNumberDesc(String templateId, Pageable pageable);

    Page<TemplateVersion> findAllByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids, Pageable pageable);

    Set<TemplateVersion> findAllByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

    List<TemplateVersion> findDistinctByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

    @Override
    default List<TemplateVersion> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }


    @Override
    default Optional<TemplateVersion> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default Optional<TemplateVersion> findFirstByName(String name) {
        return Optional.empty();
    }

    @Override
    default List<TemplateVersion> findByNameLike(String name) {
        return Collections.emptyList();
    }
}
