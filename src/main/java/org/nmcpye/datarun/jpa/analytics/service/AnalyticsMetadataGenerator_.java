//package org.nmcpye.datarun.jpa.etl.analytics.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.analytics.metadata.AllowedAggregationsResolver;
//import org.nmcpye.datarun.analytics.dto.Aggregation;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.nmcpye.datarun.jpa.etl.analytics.domain.AnalyticsAttribute;
//import org.nmcpye.datarun.jpa.etl.analytics.domain.AnalyticsSource;
//import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.AttributeScope;
//import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.AttributeType;
//import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.DataType;
//import org.nmcpye.datarun.jpa.etl.analytics.repository.AnalyticsAttributeRepository;
//import org.nmcpye.datarun.jpa.etl.analytics.repository.AnalyticsSourceRepository;
//import org.nmcpye.datarun.jpa.etl.analytics.util.AttributeValueTypeMapper;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author Hamza Assada
// * @since 12/09/2025
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AnalyticsMetadataGenerator_ {
//
//    // The well-known UID for our primary data source.
//    private static final String PIVOT_FACTS_SOURCE_UID = "src_pivot_facts_v1";
//
//    private final AnalyticsAttributeRepository attributeRepository;
//    private final AnalyticsSourceRepository sourceRepository;
//    private final ElementTemplateConfigRepository etcRepository;
//    private final AllowedAggregationsResolver allowedAggregationsResolver;
//    private final ObjectMapper objectMapper;
//
//    /**
//     * The main orchestrator method. It generates and saves all analytics attributes
//     * for a given template version in a single transaction.
//     *
//     * @param templateVersionUid The UID of the template version to process.
//     */
//    @Transactional
//    public void generateAndSaveAttributes(String templateVersionUid) {
//        log.info("Starting metadata generation for template version: {}", templateVersionUid);
//
//        AnalyticsSource pivotSource = sourceRepository.findByUid(PIVOT_FACTS_SOURCE_UID)
//            .orElseThrow(() -> new IllegalStateException("Analytics source '" + PIVOT_FACTS_SOURCE_UID + "' not found. It must be seeded in the database."));
//
//        // 1. Clean up any stale attributes for this version to ensure idempotency.
//        long deletedCount = attributeRepository.deleteAllByTemplateVersionUid(templateVersionUid);
//        if (deletedCount > 0) {
//            log.warn("Deleted {} stale attributes for template version: {}", deletedCount, templateVersionUid);
//        }
//
//        List<TemplateElement> configs = etcRepository.findByTemplateVersionUid(templateVersionUid);
//        List<AnalyticsAttribute> attributesToSave = new ArrayList<>();
//
//        // 2. Generate attributes from each TemplateElement.
//        for (TemplateElement etc : configs) {
//            if (etc.getElementKind() == TemplateElement.ElementKind.REPEAT) {
//                attributesToSave.add(createRepeatGroupAttribute(etc));
//            }
//            if (Boolean.TRUE.equals(etc.getIsDimension())) {
//                attributesToSave.add(createDimensionAttribute(etc, pivotSource));
//            }
//            if (Boolean.TRUE.equals(etc.getIsMeasure())) {
//                // A single numeric element can be aggregated in multiple ways.
//                attributesToSave.addAll(createMeasureAttributes(etc, pivotSource));
//            }
//        }
//
//        // 3. Generate system-level attributes that are common to all submissions.
//        attributesToSave.addAll(createSystemAttributes(templateVersionUid, pivotSource));
//
//        // 4. Save all new attributes to the database.
//        attributeRepository.persistAll(attributesToSave);
//        log.info("Successfully generated and saved {} attributes for template version: {}", attributesToSave.size(), templateVersionUid);
//    }
//
//    private AnalyticsAttribute createRepeatGroupAttribute(TemplateElement etc) {
//        AnalyticsAttribute attr = new AnalyticsAttribute();
//        String uid = "repeat_" + etc.getSemanticPath().replace('.', '_'); // e.g., repeat_household_members
//
//        return AnalyticsAttribute.builder()
//            .uid(uid)
//            .attributeType(AttributeType.DIMENSION)
//            .dataType(DataType.REPEAT_GROUP)
//            .attributeScope(AttributeScope.REPEAT)
//            .repeatGroupUid(findParentRepeatGroupUid(etc)) // Null if it's a top-level repeat
//
//            // Common properties
//            .templateUid(etc.getTemplateUid())
//            .templateVersionUid(etc.getTemplateVersionUid())
//            .displayName(etc.getName()) // Assuming etc has displayName
//            .displayLabels(etc.getDisplayLabel()) // Assuming etc has displayName
//            .sourceElementUid(etc.getDataElementUid())
//            .sourceSemanticPath(etc.getSemanticPath())
//            .dbMappingInfo(createJsonDbMapping("REPEAT_GROUP_INSTANCE", "instance_uid", etc.getSemanticPath()))
//            .build();
//    }
//
//    private AnalyticsAttribute createDimensionAttribute(TemplateElement etc, AnalyticsSource source) {
//        AnalyticsAttribute attr = new AnalyticsAttribute();
//        attr.setUid("dim_de_" + etc.getUid()); // e.g., dim_de_fA8w3k2Yp1
//        attr.setAttributeType(AttributeType.DIMENSION);
//
//        // --- Common Properties ---
//        attr.setTemplateVersionUid(etc.getTemplateVersionUid());
//        attr.setDisplayName(etc.getDisplayLabel());
//        attr.setSource(source);
//        attr.setSourceElementUid(etc.getUid());
//        attr.setAttributeName(etc.getName());
//
//        var attributeDataType = AttributeValueTypeMapper.map(etc.getValueType());
//        // --- Mapping Logic based on Element's Value Type ---
//        switch (attributeDataType) {
//            case TEXT:
//                attr.setDataType(DataType.TEXT);
//                // The value for this dimension is stored in the 'value_text' column of the pivot table,
//                // but only on rows where the element_uid matches this ETC's UID. The query engine will handle that filter.
//                attr.setSourceColumnMapping("value_text");
//                break;
//            case NUMBER:
//                attr.setDataType(DataType.NUMBER);
//                attr.setSourceColumnMapping("value_num");
//                break;
//            case TIMESTAMP:
//                attr.setDataType(DataType.TIMESTAMP);
//                attr.setSourceColumnMapping("value_ts");
//                break;
//            case BOOLEAN:
//                attr.setDataType(DataType.BOOLEAN);
//                attr.setSourceColumnMapping("value_bool");
//                break;
//            case ENTITY_REF:
//                attr.setDataType(DataType.ENTITY_REF);
//                attr.setSourceColumnMapping("value_ref_uid");
//                // The referenceType (e.g., 'team', 'org_unit') tells the FE which API to call for filter options.
//                attr.setEntityRefType(etc.getValueType().name());
//                break;
//            default:
//                throw new IllegalArgumentException("Unsupported dimension dataType: " + etc.getValueType());
//        }
//
//        return attr;
//    }
//
//    private List<AnalyticsAttribute> createMeasureAttributes(TemplateElement etc, AnalyticsSource source) {
//        if (!etc.getValueType().isNumeric()) {
//            return List.of();
//        }
//
//        // For a single numeric element, we create an attribute for each common aggregation.
//        return allowedAggregationsResolver.allowedFor(etc.getValueType()).stream()
//            .map(agg -> buildMeasure(etc, source, agg)).toList();
//    }
//
//
//    private AnalyticsAttribute buildMeasure(TemplateElement etc, AnalyticsSource source, Aggregation aggType) {
//        AnalyticsAttribute attr = new AnalyticsAttribute();
//        attr.setUid("meas_de_" + etc.getUid() + "_" + aggType.name().toLowerCase()); // e.g., meas_de_fA8w3k2Yp1_sum
//        attr.setAggregationType(aggType);
//
//        Map<String, String> originalName = etc.getDisplayLabel();
//        attr.setDisplayName(Map.of("en", aggType.name() + " of " + originalName.get("en"))); // Simplified localization
//
//        // --- Common Properties ---
//        attr.setAttributeType(AttributeType.MEASURE);
//        attr.setDataType(DataType.NUMBER);
//        attr.setSourceColumnMapping("value_num");
//        attr.setTemplateVersionUid(etc.getTemplateVersionUid());
//        attr.setSource(source);
//        attr.setSourceElementUid(etc.getUid());
//
//        return attr;
//    }
//
//    private List<AnalyticsAttribute> createSystemAttributes(String templateVersionUid,
//                                                            AnalyticsSource source) {
//        return List.of(
//            AnalyticsAttribute.builder()
//                .uid("meas_submission_count")
//                .displayLabels(Map.of("en", "Submission Count", "fr", "Nombre de soumissions"))
//                .sourceColumnMapping("submission_uid")
//                .attributeType(AttributeType.MEASURE)
//                .aggregationType(Aggregation.COUNT_DISTINCT) // The engine will do COUNT(DISTINCT submission_uid)
//                .dataType(DataType.NUMBER)
//                .source(source)
//                .templateVersionUid(templateVersionUid)
//                .source(source).build(),
//            AnalyticsAttribute.builder()
//                .uid("dim_org_unit_uid")
//                .displayLabels(Map.of("en", "Org Unit"))
//                .sourceColumnMapping("org_unit_uid")
//                .attributeType(AttributeType.DIMENSION)
//                .dataType(DataType.TEXT)
//                .source(source)
//                .templateVersionUid(templateVersionUid)
//                .build(),
//            AnalyticsAttribute.builder()
//                .uid("dim_team_uid")
//                .displayLabels(Map.of("en", "Team"))
//                .sourceColumnMapping("team_uid")
//                .attributeType(AttributeType.DIMENSION)
//                .dataType(DataType.TEXT)
//                .source(source)
//                .templateVersionUid(templateVersionUid)
//                .build(),
//            AnalyticsAttribute.builder()
//                .uid("dim_activity_uid")
//                .displayLabels(Map.of("en", "Activity"))
//                .sourceColumnMapping("activity_uid")
//                .dataType(DataType.TEXT)
//                .attributeType(AttributeType.DIMENSION)
//                .source(source)
//                .templateVersionUid(templateVersionUid)
//                .build(),
//
//            AnalyticsAttribute.builder()
//                .uid("dim_activity_uid")
//                .displayLabels(Map.of("en", "Activity"))
//                .sourceColumnMapping("activity_uid")
//                .dataType(DataType.TEXT)
//                .attributeType(AttributeType.DIMENSION)
//                .source(source)
//                .templateVersionUid(templateVersionUid)
//                .build(),
//
//            AnalyticsAttribute.builder()
//                .uid("dim_activity_uid")
//                .displayName(Map.of("en", "Activity"))
//                .sourceColumnMapping("activity_uid")
//                .dataType(DataType.TEXT)
//                .attributeType(AttributeType.DIMENSION)
//                .source(source)
//                .templateVersionUid(templateVersionUid)
//                .build()
//        );
//    }
//
//
//    // --- Utility Methods ---
//    private String findParentRepeatGroupUid(TemplateElement etc) {
//        // This method would traverse up the `etc` hierarchy until it finds the parent
//        // that has `isRepeat() == true` and returns its generated UID.
//        // For now, a placeholder:
//        if (etc.getAncestorRepeatSemanticPath() != null) {
//            return "repeat_" + etc.getAncestorRepeatSemanticPath().replace('.', '_');
//        }
//        return null;
//    }
//
//    private String createJsonDbMapping(String sourceTable, String sourceColumn, String elementUid) {
//        ObjectNode mapping = objectMapper.createObjectNode();
//        mapping.put("source", sourceTable);
//        mapping.put("column", sourceColumn);
//        if (elementUid != null) {
//            mapping.put("element_uid", elementUid); // Used by the engine to filter the pivot table
//        }
//        return mapping.toString();
//    }
//
//    private String referenceTableFor(ValueType vt) {
//        if (vt == null) return null;
//        return switch (vt) {
//            case Activity -> "activity";
//            case Team -> "team";
//            case OrganisationUnit -> "org_unit";
//            case Entity -> "entity_instance";
//            case SelectOne, SelectMulti -> "option_value";
//            default -> null;
//        };
//    }
//}
