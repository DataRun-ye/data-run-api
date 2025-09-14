package org.nmcpye.datarun.analytics.metadata;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.dto.QueryableElement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 14/09/2025
 */
@Service
@RequiredArgsConstructor
public class MetadataResolver {
    private final MetadataService metadataService;

    public Map<String, QueryableElement> getMetadataMapForTemplate(String templateUid, String templateVersionUid) {
        Map<String, QueryableElement> fieldMap = new ConcurrentHashMap<>(metadataService
            .getMetadataForTemplate(templateUid, templateVersionUid)
            .getAvailableFields().stream()
            .collect(Collectors.toMap(QueryableElement::id, f -> f)));

        return Map.of();
    }

    /**
     * REFACTORED: This method is now much simpler. It leverages the main, cached
     * getMetadataForTemplate method and filters the result. This eliminates
     * code duplication and ensures consistency.
     */
    @Transactional(readOnly = true)
    public Optional<QueryableElement> resolveFieldById(String standardizedId, String templateUid, String templateVersionUid) {
        if (standardizedId == null) return Optional.empty();
        return Optional.ofNullable(getMetadataMapForTemplate(templateUid, templateVersionUid)
            .get(standardizedId));
    }
}
