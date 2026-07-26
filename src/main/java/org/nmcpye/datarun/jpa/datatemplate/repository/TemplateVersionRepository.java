package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    List<TemplateVersion> findDistinctByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

    @Query("""
        select tv
        from TemplateVersion tv
        where tv.templateUid in :templateUids
          and tv.versionNumber = (
              select max(candidate.versionNumber)
              from TemplateVersion candidate
              where candidate.templateUid = tv.templateUid
          )
        """)
    List<TemplateVersion> findLatestByTemplateUidIn(
        @Param("templateUids") Collection<String> templateUids);

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
