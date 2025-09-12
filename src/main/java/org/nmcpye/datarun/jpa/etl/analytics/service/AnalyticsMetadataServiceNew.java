package org.nmcpye.datarun.jpa.etl.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.etl.analytics.domain.AnalyticsAttribute;
import org.nmcpye.datarun.jpa.etl.analytics.dto.AnalyticsAttributeDto;
import org.nmcpye.datarun.jpa.etl.analytics.dto.AnalyticsAttributeMapper;
import org.nmcpye.datarun.jpa.etl.analytics.repository.AnalyticsAttributeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 07/09/2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsMetadataServiceNew {
    private final AnalyticsAttributeMapper analyticsAttributeMapper;

    public static final String METADATA_CACHE_NAME = "analytics-metadata";

    private final AnalyticsAttributeRepository attributeRepository;

    /**
     * Retrieves all analytics attributes for a given template version as DTOs.
     * The result of this method is cached. The first call for a specific templateVersionUid
     * will hit the database. Subsequent calls with the same UID will return the cached result
     * directly, providing a massive performance boost.
     *
     * @param templateVersionUid The UID of the template version.
     * @return A list of DTOs for the client.
     */
    @Cacheable(value = METADATA_CACHE_NAME, key = "#templateVersionUid")
    @Transactional(readOnly = true) // Use a read-only transaction for performance
    public List<AnalyticsAttributeDto> getAttributes(String templateVersionUid) {
        log.info("CACHE MISS: Fetching attributes for template version {} from database.", templateVersionUid);
        List<AnalyticsAttribute> attributes = attributeRepository.findByTemplateVersionUid(templateVersionUid);
        return attributes.stream().map(analyticsAttributeMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Provides the same data as getAttributes, but in a Map for efficient lookups by UID.
     * This will be used internally by the query engine in the next step.
     * The result is also cached.
     *
     * @param templateVersionUid The UID of the template version.
     * @return A map of attribute UID to the full AnalyticsAttribute entity.
     */
    @Cacheable(value = METADATA_CACHE_NAME, key = "'map-' + #templateVersionUid")
    @Transactional(readOnly = true)
    public Map<String, AnalyticsAttribute> getAttributesAsMap(String templateVersionUid) {
        log.info("CACHE MISS: Fetching attribute map for template version {} from database.", templateVersionUid);
        return attributeRepository.findByTemplateVersionUid(templateVersionUid)
            .stream()
            .collect(Collectors.toMap(AnalyticsAttribute::getUid, Function.identity()));
    }

    /**
     * A helper method to evict the cache for a specific template version.
     * This should be called after the metadata generation is complete to ensure
     * the cache is not stale.
     *
     * @param templateVersionUid The UID of the template version whose cache to clear.
     */
    @CacheEvict(value = METADATA_CACHE_NAME, allEntries = true) // For simplicity, clear all. Can be targeted.
    public void clearMetadataCache() {
        log.warn("Clearing all entries from the analytics metadata cache.");
    }
}
