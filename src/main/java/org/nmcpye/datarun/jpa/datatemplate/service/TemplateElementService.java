package org.nmcpye.datarun.jpa.datatemplate.service;

import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.stream.Collectors;

/// @author Hamza Assada
/// @since 10/08/2025
@Service
public class TemplateElementService {
    public final static String TEMPLATE_MAP_CACHE = "templateMapCacheByTemplateAndVersion";

    private final DataTemplateInstanceService templateInstanceService;
    private final ElementTemplateConfigRepository templateConfigRepository;

    public TemplateElementService(DataTemplateInstanceService templateInstanceService, ElementTemplateConfigRepository templateConfigRepository) {
        this.templateInstanceService = templateInstanceService;
        this.templateConfigRepository = templateConfigRepository;
    }

    /// create elementMap and cache it.
    ///
    /// @param id         template id
    /// @param versionUid template version id
    /// @return elementMap cache;
    @Cacheable(cacheNames = TEMPLATE_MAP_CACHE)
    public TemplateElementMap getTemplateElementMap(String id, String versionUid) {
        final var elementsConfMap =
            templateConfigRepository.findAllByTemplateUidAndTemplateVersionUid(id, versionUid).stream().collect(Collectors.toMap(
                ElementTemplateConfig::getNamePath, Function.identity()));
        return new TemplateElementMap(templateInstanceService.findByTemplateAndVersionUid(id, versionUid)
            .orElseThrow(), elementsConfMap);
    }

    public TemplateElementMap getTemplateElementMap(String id, Integer version) {
        final var elementsConfMap =
            templateConfigRepository.findAllByTemplateUidAndVersionNo(id, version).stream().collect(Collectors.toMap(
                ElementTemplateConfig::getNamePath, Function.identity()));
        return new TemplateElementMap(templateInstanceService.findByTemplateAndVersionNo(id, version)
            .orElseThrow(), elementsConfMap);
    }
}
