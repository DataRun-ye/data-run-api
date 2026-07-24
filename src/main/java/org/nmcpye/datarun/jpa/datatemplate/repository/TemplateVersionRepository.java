package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

/// Spring Data jpa repository for the DataTemplate entity.
@Repository
public interface TemplateVersionRepository
    extends JpaIdentifiableRepository<TemplateVersion> {
    String TEMPLATE_UID_VERSION_NO_JPA_CACHE = "templateUidVersionNoCache";
    String TEMPLATE_UID_VERSION_UID_JPA_CACHE = "templateUidVersionUidCache";
    String TEMPLATE_UID_LATEST_VERSION_JPA_CACHE = "templateUidLatestVersionCache";

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

    List<TemplateVersion> findDistinctByTemplateUidInOrderByVersionNumberDesc(Collection<String> uids);

    List<TemplateVersion> findByTemplateUidIn(Collection<String> templateUids);

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
    List<TemplateVersion> findLatestByTemplateUidIn(@Param("templateUids") Collection<String> templateUids);
}
