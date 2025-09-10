package org.nmcpye.datarun.analytics.pivotg;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.pivot.AllowedAggregationsResolver;
import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
import org.nmcpye.datarun.analytics.pivot.dto.DataType;
import org.nmcpye.datarun.analytics.pivot.dto.FieldCategory;
import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
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
    private final PivotFieldMapper pivotFieldMapper;
    private final AllowedAggregationsResolver aggrResolver;

    /**
     * Generates a combined list of metadata fields for the pivot table.
     * The result is cached based on the formTemplateUid (or a "global" key if absent).
     */
    @Cacheable(value = METADATA_CACHE, key = "#dataTemplateUid.orElse('global')" +
        " + (#dataTemplateUid.present ? '::' + #templateVersionUid.orElse('latest') : '')")
    public List<PivotFieldDto> generateMetadata(Optional<String> dataTemplateUid, Optional<String> templateVersionUid) {
        List<PivotFieldDto> coreDimensions = generateCoreDimensionMetadata();
        List<PivotFieldDto> hierarchicalContext = generateHierarchicalContextMetadata();
        List<PivotFieldDto> dynamicMeasures = dataTemplateUid
            .map(dt -> generateTemplateModeMeasures(dataTemplateUid.get(),
                templateVersionUid.orElse(null)))      // If UID is present, call this
            .orElseGet(this::generateGlobalModeMeasures); // If UID is absent, call this

        // Combine all lists into a single response
        return Stream.of(coreDimensions, hierarchicalContext, dynamicMeasures)
            .flatMap(List::stream)
            .toList();
    }

    private List<PivotFieldDto> generateCoreDimensionMetadata() {
        // This can be a static or semi-static list, as core dimensions rarely change.
        // For simplicity, creating them here. In a real app, this might come from a config.
        return List.of(
            PivotFieldDto.builder()
                .id("team_uid")
                .label("Team")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType(DataType.UID)
                .factColumn("team_uid")
                .aggregationModes(aggrResolver.allowedFor(ValueType.Team))
//                .templateModeOnly(false).source("system")
                .build(),

            PivotFieldDto.builder()
                .id("org_unit_uid")
                .label("Org Unit")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType(DataType.UID)
                .factColumn("org_unit_uid")
                .aggregationModes(aggrResolver
                    .allowedFor(ValueType.OrganisationUnit))
//                .templateModeOnly(false)
//                .source("system")
                .build(),

            PivotFieldDto.builder()
                .id("activity_uid")
                .label("Activity")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType(DataType.UID)
                .factColumn("activity_uid")
                .aggregationModes(aggrResolver
                    .allowedFor(ValueType.Activity))
//                .templateModeOnly(false)
//                .source("system")
                .build(),

            PivotFieldDto.builder()
                .id("submission_completed_at")
                .label("Submission completed at")
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

    private List<PivotFieldDto> generateHierarchicalContextMetadata() {
        List<PivotFieldDto> fields = new ArrayList<>();

        fields.add(PivotFieldDto.builder()
            .id("category:child")
            .category(FieldCategory.HIERARCHICAL_CONTEXT)
            .label("Category (Level 1)")
            .dataType(DataType.UID)
            .factColumn("child_category_uid")
            .aggregationModes(Set.
                of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//            .templateModeOnly(false)
//            .source("system")
            .build());
        fields.add(PivotFieldDto.builder()
            .id("category:parent")
            .category(FieldCategory.HIERARCHICAL_CONTEXT)
            .label("Category (Level 2)")
            .dataType(DataType.UID)
            .factColumn("parent_category_uid")
            .aggregationModes(Set.
                of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//            .templateModeOnly(false)
//            .source("system")
            .build());

        fields.add(PivotFieldDto.builder()
            .id("repeat_path")
            .category(FieldCategory.HIERARCHICAL_CONTEXT)
            .label("Category (Level 2)")
            .dataType(DataType.TEXT)
            .factColumn("repeat_path")
            .aggregationModes(Set.
                of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//            .templateModeOnly(true)
//            .source("system")
            .build());// Template mode only
        return fields;
    }

    private List<PivotFieldDto> generateGlobalModeMeasures() {
        // Fetch all distinct data_elements that have been used.
        var dataElements = analyticsMetadataRepository.findUsedDataElements();
        // Use MapStruct to convert the list of entities/records to DTOs
        return pivotFieldMapper.toPivotFieldDTOs(dataElements);
    }

    private List<PivotFieldDto> generateTemplateModeMeasures(String templateUid, String templateVersionUid) {
        // Fetch all element_template_configs for the given form.
        var configs =
            analyticsMetadataRepository
                .findElementConfigsByTemplate(templateUid, templateVersionUid);
        // MapStruct conversion
        return pivotFieldMapper.toPivotFieldDTOsFromConfigs(configs);
    }
}
