package org.nmcpye.datarun.analytics.pivotg;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.AllowedAggregationsResolver;
import org.nmcpye.datarun.analytics.dto.Aggregation;
import org.nmcpye.datarun.analytics.dto.DataType;
import org.nmcpye.datarun.analytics.dto.FieldCategory;
import org.nmcpye.datarun.analytics.dto.QueryableElement;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Hamza Assada
 * @since 26/08/2025
 */

@Service
@RequiredArgsConstructor
public class AnalyticsMetadataService {

    // Using a constant for the cache name is a good practice.
    public static final String METADATA_CACHE = "analyticsMetadataCache";

    private final AnalyticsMetadataRepository analyticsMetadataRepository;
    private final QueryableElementMapper queryableElementMapper;
    private final AllowedAggregationsResolver aggrResolver;

    /**
     * Generates a combined list of metadata fields for the pivot table.
     * The result is cached based on the formTemplateUid (or a "global" key if absent).
     */
    @Cacheable(value = METADATA_CACHE, key = "#dataTemplateUid.orElse('global')" +
        " + (#dataTemplateUid.present ? '::' + #templateVersionUid.orElse('latest') : '')")
    public List<QueryableElement> generateMetadata(Optional<String> dataTemplateUid, Optional<String> templateVersionUid) {
        List<QueryableElement> coreDimensions = generateCoreDimensionMetadata();
        List<QueryableElement> hierarchicalContext = generateHierarchicalContextMetadata();
        List<QueryableElement> dynamicMeasures = dataTemplateUid
            .map(dt -> generateTemplateModeMeasures(dataTemplateUid.get(),
                templateVersionUid.orElse(null)))      // If UID is present, call this
            .orElseGet(this::generateGlobalModeMeasures); // If UID is absent, call this

        // Combine all lists into a single response
        return Stream.of(coreDimensions, hierarchicalContext, dynamicMeasures)
            .flatMap(List::stream)
            .toList();
    }

    private List<QueryableElement> generateCoreDimensionMetadata() {
        // This can be a static or semi-static list, as core dimensions rarely change.
        // For simplicity, creating them here. In a real app, this might come from a config.
        return List.of(
            QueryableElement.builder()
                .id("team_uid")
                .name("Team")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType(DataType.UID)
                .factColumn("team_uid")
                .aggregationModes(aggrResolver.allowedFor(ValueType.Team))
//                .templateModeOnly(false).source("system")
                .build(),

            QueryableElement.builder()
                .id("org_unit_uid")
                .name("Org Unit")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType(DataType.UID)
                .factColumn("org_unit_uid")
                .aggregationModes(aggrResolver
                    .allowedFor(ValueType.OrganisationUnit))
//                .templateModeOnly(false)
//                .source("system")
                .build(),

            QueryableElement.builder()
                .id("activity_uid")
                .name("Activity")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType(DataType.UID)
                .factColumn("activity_uid")
                .aggregationModes(aggrResolver
                    .allowedFor(ValueType.Activity))
//                .templateModeOnly(false)
//                .source("system")
                .build(),

            QueryableElement.builder()
                .id("submission_completed_at")
                .name("Submission completed at")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType(DataType.TIMESTAMP)
                .factColumn("submission_completed_at")
                .aggregationModes(Set.of(Aggregation.MIN,
                    Aggregation.MAX))
//                .templateModeOnly(false)
//                .source("system")
                .build()
        );
    }

    private List<QueryableElement> generateHierarchicalContextMetadata() {
        List<QueryableElement> fields = new ArrayList<>();

        fields.add(QueryableElement.builder()
            .id("category:child")
            .category(FieldCategory.HIERARCHICAL_CONTEXT)
            .name("Category (Level 1)")
            .dataType(DataType.UID)
            .factColumn("child_category_uid")
            .aggregationModes(Set.
                of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//            .templateModeOnly(false)
//            .source("system")
            .build());
        fields.add(QueryableElement.builder()
            .id("category:parent")
            .category(FieldCategory.HIERARCHICAL_CONTEXT)
            .name("Category (Level 2)")
            .dataType(DataType.UID)
            .factColumn("parent_category_uid")
            .aggregationModes(Set.
                of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//            .templateModeOnly(false)
//            .source("system")
            .build());

        fields.add(QueryableElement.builder()
            .id("repeat_path")
            .category(FieldCategory.HIERARCHICAL_CONTEXT)
            .name("Category (Level 2)")
            .dataType(DataType.TEXT)
            .factColumn("repeat_path")
            .aggregationModes(Set.
                of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//            .templateModeOnly(true)
//            .source("system")
            .build());// Template mode only
        return fields;
    }

    private List<QueryableElement> generateGlobalModeMeasures() {
        // Fetch all distinct data_elements that have been used.
        var dataElements = analyticsMetadataRepository.findUsedDataElements();
        // Use MapStruct to convert the list of entities/records to DTOs
        return queryableElementMapper.toQueryableElement(dataElements);
    }

    private List<QueryableElement> generateTemplateModeMeasures(String templateUid, String templateVersionUid) {
        // Fetch all element_template_configs for the given form.
        var configs =
            analyticsMetadataRepository
                .findElementConfigsByTemplate(templateUid, templateVersionUid);
        // MapStruct conversion
        return queryableElementMapper.toQueryableElementsFromConfigs(configs);
    }
}
