//package org.nmcpye.datarun.analytics.pivot.service;
//
//import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDTO;
//import org.nmcpye.datarun.analytics.pivot.repository.AnalyticsMetadataRepository;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Stream;
//
///**
// * @author Hamza Assada
// * @since 26/08/2025
// */
//
//@Service
//public class AnalyticsMetadataService {
//
//    // Using a constant for the cache name is a good practice.
//    public static final String METADATA_CACHE = "analyticsMetadataCache";
//
//    private final AnalyticsMetadataRepository analyticsMetadataRepository;
//    private final PivotFieldMapper pivotFieldMapper;
//
//    public AnalyticsMetadataService(AnalyticsMetadataRepository repo, PivotFieldMapper mapper) {
//        this.analyticsMetadataRepository = repo;
//        this.pivotFieldMapper = mapper;
//    }
//
//    /**
//     * Generates a combined list of metadata fields for the pivot table.
//     * The result is cached based on the formTemplateUid (or a "global" key if absent).
//     */
//    @Cacheable(value = METADATA_CACHE, key = "#formTemplateUid.orElse('global')")
//    public List<PivotFieldDTO> generateMetadata(Optional<String> formTemplateUid) {
//        List<PivotFieldDTO> coreDimensions = generateCoreDimensionMetadata();
//        List<PivotFieldDTO> hierarchicalContext = generateHierarchicalContextMetadata();
//        List<PivotFieldDTO> dynamicMeasures = formTemplateUid
//            .map(this::generateTemplateModeMeasures)      // If UID is present, call this
//            .orElseGet(this::generateGlobalModeMeasures); // If UID is absent, call this
//
//        // Combine all lists into a single response
//        return Stream.of(coreDimensions, hierarchicalContext, dynamicMeasures)
//            .flatMap(List::stream)
//            .toList();
//    }
//
//    private List<PivotFieldDTO> generateCoreDimensionMetadata() {
//        // This can be a static or semi-static list, as core dimensions rarely change.
//        // For simplicity, creating them here. In a real app, this might come from a config.
//        List<PivotFieldDTO> fields = new ArrayList<>();
//        fields.add(new PivotFieldDTO("org_unit_uid", "Organization Unit", PivotFieldCategory.CORE_DIMENSION, FieldValueType.REFERENCE, "org_unit_id", List.of(AggregationType.COUNT_DISTINCT), AggregationType.COUNT_DISTINCT, false));
//        fields.add(new PivotFieldDTO("team_uid", "Team", PivotFieldCategory.CORE_DIMENSION, FieldValueType.REFERENCE, "team_id", List.of(AggregationType.COUNT_DISTINCT), AggregationType.COUNT_DISTINCT, false));
//        fields.add(new PivotFieldDTO("activity_uid", "Activity", PivotFieldCategory.CORE_DIMENSION, FieldValueType.REFERENCE, "activity_id", List.of(AggregationType.COUNT_DISTINCT), AggregationType.COUNT_DISTINCT, false));
//        fields.add(new PivotFieldDTO("submission_completed_at", "Submission Date", PivotFieldCategory.CORE_DIMENSION, FieldValueType.TIMESTAMP, "submission_completed_at", List.of(AggregationType.COUNT), AggregationType.COUNT, false));
//        return fields;
//    }
//
//    private List<PivotFieldDTO> generateHierarchicalContextMetadata() {
//        List<PivotFieldDTO> fields = new ArrayList<>();
//        fields.add(new PivotFieldDTO("category:child", "Category (Level 1)", PivotFieldCategory.HIERARCHICAL_CONTEXT, FieldValueType.REFERENCE, "child_category_id", List.of(AggregationType.COUNT_DISTINCT), AggregationType.COUNT_DISTINCT, false));
//        fields.add(new PivotFieldDTO("category:parent", "Category (Level 2)", PivotFieldCategory.HIERARCHICAL_CONTEXT, FieldValueType.REFERENCE, "parent_category_id", List.of(AggregationType.COUNT_DISTINCT), AggregationType.COUNT_DISTINCT, false));
//        fields.add(new PivotFieldDTO("repeat_path", "Repeat Path", PivotFieldCategory.HIERARCHICAL_CONTEXT, FieldValueType.TEXT, "repeat_path", List.of(AggregationType.COUNT_DISTINCT), AggregationType.COUNT_DISTINCT, true)); // Template mode only
//        return fields;
//    }
//
//    private List<PivotFieldDTO> generateGlobalModeMeasures() {
//        // Fetch all distinct data_elements that have been used.
//        var dataElements = analyticsMetadataRepository.findUsedDataElements();
//        // Use MapStruct to convert the list of entities/records to DTOs
//        return pivotFieldMapper.toPivotFieldDTOs(dataElements);
//    }
//
//    private List<PivotFieldDTO> generateTemplateModeMeasures(String formTemplateUid) {
//        // Fetch all element_template_configs for the given form.
//        var configs = analyticsMetadataRepository.findElementConfigsByTemplate(formTemplateUid);
//        // MapStruct conversion
//        return pivotFieldMapper.toPivotFieldDTOsFromConfigs(configs);
//    }
//}
