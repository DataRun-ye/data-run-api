//package org.nmcpye.datarun.jpa.pivot;
//
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
///**
// * Loads and caches template_element metadata for templates.
// * Uses Spring @Cacheable — make sure cache name "pivotMetadata" is configured in Ehcache.
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@Service
//@RequiredArgsConstructor
//public class PivotMetadataService {
//
//    public static final String PIVOT_METADATA_CACHE = "pivotMetadata";
//
//    private final ElementTemplateConfigRepository elementTemplateConfigRepository;
//
//    /**
//     * Load and cache metadata for given templateId + versionNo.
//     * Use templateVersionNo == null to fetch latest (repository method may be adapted).
//     */
//    @Cacheable(cacheNames = PIVOT_METADATA_CACHE, key = "#templateId + '::' + (#versionNo == null ? 'latest' : #versionNo)")
//    public List<TemplateElement> getFieldsForTemplate(String templateId, Integer versionNo) {
//        // repository method - adapt if your repo uses different signature for latest version.
//        List<TemplateElement> rows = elementTemplateConfigRepository.findAllByTemplateIdAndVersionNo(templateId, versionNo);
//        if (rows == null) return Collections.emptyList();
//        return Collections.unmodifiableList(rows);
//    }
//
//    /**
//     * Convenience: get element config by elementId (dataElementId) or by element config name.
//     */
//    public Optional<TemplateElement> getField(String templateId, Integer versionNo, String elementIdOrName) {
//        return getFieldsForTemplate(templateId, versionNo).stream()
//                .filter(cfg -> elementIdOrName != null &&
//                        (elementIdOrName.equals(cfg.getDataElementId()) || elementIdOrName.equals(cfg.getName())))
//                .findFirst();
//    }
//
//    /**
//     * Map elementId -> config for quick lookups.
//     */
//    public Map<String, TemplateElement> getFieldMap(String templateId, Integer versionNo) {
//        return getFieldsForTemplate(templateId, versionNo).stream()
//                .collect(Collectors.toMap(TemplateElement::getDataElementId, c -> c));
//    }
//}
