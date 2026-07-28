package org.nmcpye.datarun.jpa.datatemplate.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Resolves the exact immutable template version used by runtime submission
 * processing. Generated ETL metadata is not part of this boundary.
 */
@Service
@RequiredArgsConstructor
public class TemplateVersionResolver {
    public static final String TEMPLATE_VERSION_CONTEXT_CACHE =
        "templateVersionContextByTemplateAndVersion";

    private final DataTemplateInstanceService templateInstanceService;

    @Cacheable(cacheNames = TEMPLATE_VERSION_CONTEXT_CACHE)
    public TemplateVersionContext resolveByUid(
        String templateUid,
        String versionUid) {
        return new TemplateVersionContext(templateInstanceService
            .findByTemplateAndVersionUid(templateUid, versionUid)
            .orElseThrow());
    }

    @Cacheable(cacheNames = TEMPLATE_VERSION_CONTEXT_CACHE)
    public TemplateVersionContext resolveByNumber(
        String templateUid,
        Integer versionNumber) {
        return new TemplateVersionContext(templateInstanceService
            .findByTemplateAndVersionNo(templateUid, versionNumber)
            .orElseThrow());
    }
}
