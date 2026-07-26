package org.nmcpye.datarun.jpa.datatemplate.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

/// @author Hamza Assada
/// @since 10/08/2025
@Service
@RequiredArgsConstructor
public class TemplateElementService {
    public final static String TEMPLATE_MAP_CACHE = "templateMapCacheByTemplateAndVersion";

    private final DataTemplateInstanceService templateInstanceService;

    /// create elementMap and cache it.
    ///
    /// @param id         template id
    /// @param versionUid template version id
    /// @return elementMap cache;
    @Cacheable(cacheNames = TEMPLATE_MAP_CACHE)
    public TemplateElementMap getTemplateElementMap(String id, String versionUid) {
        return new TemplateElementMap(templateInstanceService.findByTemplateAndVersionUid(id, versionUid)
            .orElseThrow(), Map.of());
    }

    public TemplateElementMap getTemplateElementMap(String id, Integer version) {
        return new TemplateElementMap(templateInstanceService.findByTemplateAndVersionNo(id, version)
            .orElseThrow(), Map.of());
    }
}
