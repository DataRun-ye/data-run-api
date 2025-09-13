package org.nmcpye.datarun.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.analytics.dto.DataType;
import org.nmcpye.datarun.analytics.dto.MetadataResponse;
import org.nmcpye.datarun.analytics.dto.QueryableElement;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation notes for PivotMetadataServiceImpl:
 * <p>
 * - Should load all ElementTemplateConfig rows for templateUid+templateVersionUid, batch-load their DataElement metadata,
 * and produce a list of QueryableElement objects:
 * <pre>
 * - QueryableElement.sourceColumn must map to the materialized view column (e.g., "etc_uid", "value_num", "option_uid", "value_ts", "team_uid").
 * - QueryableElement.dataType uses the "value_num"/"option_uid"/"value_ts"/"value_bool"/"value_text" nomenclature.
 * - aggregationModes should be computed via AllowedAggregationsResolver based on DataElement.valueType.
 * </pre>
 * <p>
 * - Cache the assembled PivotMetadataResponse with @Cacheable. Invalidation should occur when a template/version is updated/published.
 * <p>
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataServiceImpl implements MetadataService {

    private final ElementTemplateConfigRepository etcRepository;
    private final DataElementRepository dataElementRepository;
    private final AllowedAggregationsResolver aggrResolver;

    private static final String CACHE_PREFIX = "pivot.metadata:template:";
    final public static String PIVOT_CACHE_NAME = "pivot.metadata";

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheNames = PIVOT_CACHE_NAME, key = "T(java.lang.String).valueOf('" + CACHE_PREFIX + "').concat(#templateUid).concat(':').concat(#templateVersionUid)")
    public MetadataResponse getMetadataForTemplate(String templateUid, String templateVersionUid) {
        Objects.requireNonNull(templateUid, "templateUid is required");
        Objects.requireNonNull(templateVersionUid, "templateVersionUid is required");

        List<ElementTemplateConfig> etcRows = etcRepository.findAllByTemplateUidAndTemplateVersionUid(templateUid, templateVersionUid);

        Set<String> dataElementUids = etcRows.stream()
            .map(ElementTemplateConfig::getDataElementUid)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<String, DataElement> dataElementMap = dataElementUids.isEmpty()
            ? Collections.emptyMap()
            : dataElementRepository.findAllByUidIn(dataElementUids)
            .stream().collect(Collectors.toMap(DataElement::getUid, de -> de));

        // STEP 1: Build DTOs for template-specific fields.
        List<QueryableElement> templateFields = etcRows.stream().map(etc -> {
            if (etc.getElementKind() == ElementTemplateConfig.ElementKind.REPEAT) {
                return buildForRepeat(etc);
            }
            return buildForField(etc, dataElementMap);
        }).toList();

        // STEP 2: Combine with core dimensions into a single list.
        List<QueryableElement> allFields = Stream.concat(getCoreDimensions()
                .stream(), templateFields.stream())
            .collect(Collectors.toList());

        // STEP 3: Return the new, unified response object.
        return MetadataResponse.builder()
            .availableFields(allFields)
            .hints(Map.of(
                "templateUid", templateUid,
                "templateVersionUid", templateVersionUid))
            .build();
    }

    @Override
    public Map<String, QueryableElement> getMetadataMapForTemplate(String templateUid, String templateVersionUid) {
        return Map.of();
    }

    private QueryableElement buildForRepeat(ElementTemplateConfig etc) {
        Map<String, Object> extras = buildExtras(etc, null); // Pass DE for richer extras

        return QueryableElement.builder()
            .id("etc:" + etc.getSemanticPath()) // NEW: Standardized ID
            .name(etc.getName())
            .label(etc.getDisplayLabel())
//            .dataType(resolveDataType(etc, de))
            .sourceColumn("semantic_path") // The column used for predicates and grouping
            .displayGroup(etc.getAncestorRepeatSemanticPath() != null ?
                etc.getAncestorRepeatSemanticPath() : "Template")
            .isSortable(false)
            .isDimension(true)
            .aggregationModes(Collections.emptySet())
            .extras(extras)
            .build();
    }

    private QueryableElement buildForField(ElementTemplateConfig etc, Map<String, DataElement> dataElementMap) {
        DataElement de = dataElementMap.get(etc.getDataElementUid());

        Map<String, Object> extras = buildExtras(etc, de); // Pass DE for richer extras

        // Add resolution metadata for the UI
        if (de != null && de.getOptionSet() != null) {
            extras.put("resolution", Map.of(
                "type", "API_ENDPOINT",
                "endpoint", String.format("/api/v1/optionSets/%s/values", de.getOptionSet().getUid())
            ));
        }

        return QueryableElement.builder()
            .id("etc:" + etc.getUid()) // NEW: Standardized ID
            .name(etc.getName())
            .label(etc.getDisplayLabel())
            .dataType(resolveDataType(etc, de))
            .sourceColumn("etc_uid") // The column used for predicates and grouping
            .deUid(etc.getDataElementUid())
            .displayGroup(etc.getAncestorRepeatSemanticPath() != null ?
                etc.getAncestorRepeatSemanticPath() : "Template")
            .isSortable(etc.getValueType().isNumeric())
            .isDimension(etc.getIsDimension())
            .aggregationModes(aggrResolver.allowedFor(de != null ? de.getValueType() : null))
            .extras(extras)
            .build();
    }

    /**
     * REFACTORED: This method is now much simpler. It leverages the main, cached
     * getMetadataForTemplate method and filters the result. This eliminates
     * code duplication and ensures consistency.
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<QueryableElement> resolveFieldById(String standardizedId, String templateUid, String templateVersionUid) {
        if (standardizedId == null) return Optional.empty();

        // Call the main method (which will hit the cache) and find the field.
        return getMetadataForTemplate(templateUid, templateVersionUid)
            .getAvailableFields().stream()
            .filter(field -> standardizedId.equals(field.id()))
            .findFirst();
    }

    /**
     * NEW: Helper to build the core dimensions list with standardized IDs and resolution metadata.
     */
    private List<QueryableElement> getCoreDimensions() {
        return List.of(
            createCoreDimension("team_uid", "Team",
                ValueType.Team, "/api/v1/teams"),
            createCoreDimension("org_unit_uid", "Org Unit",
                ValueType.OrganisationUnit, "/api/v1/orgUnits"),
            createCoreDimension("activity_uid", "Activity",
                ValueType.Activity, "/api/v1/activities"),
            createCoreDimension("submission_completed_at", "Submission Date",
                ValueType.DateTime, null),
            // NEW: Add Hierarchical Dimensions
            createCoreDimension("child_category_name", "Child Category Name",
                ValueType.Text, null),
            createCoreDimension("parent_category_name", "Parent Category Name",
                ValueType.Text, null)
        );
    }

    /**
     * NEW: A factory method to consistently create Core Dimension DTOs.
     */
    private QueryableElement createCoreDimension(String factColumn, String name,
                                                 ValueType valueType, String resolutionEndpoint) {
        Map<String, Object> extras = new HashMap<>();
        // This 'resolution' map is key to making the UI "dumber" and more data-driven.
        if (resolutionEndpoint != null) {
            extras.put("resolution", Map.of(
                "type", "API_ENDPOINT",
                "endpoint", resolutionEndpoint
            ));
        }

        return QueryableElement.builder()
            .id("core:" + factColumn) // NEW: Standardized ID
            .name(name)
            .label(Map.of("en", name))
            .dataType(mapValueTypeToDataType(valueType)) // Use consistent mapping
            .sourceColumn(factColumn) // For core dimensions, the sourceColumn is the ID itself
            .aggregationModes(aggrResolver.allowedFor(valueType))
            .isDimension(true)
            .isSortable(true)
            .displayGroup("Domain DIMENSION")
            .extras(extras)
            .build();
    }

    /**
     * NEW: Helper to build the extras map, now including resolution info for options.
     */
    private Map<String, Object> buildExtras(ElementTemplateConfig etc, DataElement de) {
        Map<String, Object> extras = new HashMap<>();
        extras.put("isMulti", Boolean.TRUE.equals(etc.getIsMulti()));
        extras.put("isReference", Boolean.TRUE.equals(etc.getIsReference()));
        extras.put("isRepeat", etc.getElementKind() == ElementTemplateConfig.ElementKind.REPEAT);
        extras.put("repeatPath", etc.getAncestorRepeatPath());
        extras.put("semanticRepeatPath", etc.getAncestorRepeatSemanticPath());
        extras.put("semanticPath", etc.getSemanticPath());

        // Add resolution metadata for Option Sets to drive the UI
        if (de != null && de.getOptionSet() != null) {
            extras.put("resolution", Map.of(
                "type", "API_ENDPOINT",
                "endpoint", String.format("/api/v1/optionSets/%s/values", de.getOptionSet().getUid())
            ));
        }

        // --- NEW LOGIC FOR RECOMMENDATIONS ---
        // Check if the field is in a repeatable section AND that section has a category defined.
        if (Boolean.TRUE.equals(etc.getHasRepeatAncestor()) && etc.getCategoryId() != null && !etc.getCategoryId().isBlank()) {

            // If so, add the "recommendedDimensions" hint to the extras.
            // We recommend grouping by "child_category_name" because the ETL process
            // denormalizes the category's friendly name into this column in the materialized view.
            // We use the standardized ID as this is what the client will use in its query.
            extras.put("recommendedDimensions", List.of("core:child_category_name"));
        }
        return extras;
    }

    /**
     * Decide which pivot dataType to expose for a given template config and optional DataElement meta.
     * <p>
     * This method returns one of the pivot dataType strings used across the analytics stack:
     * - "value_num", "value_bool", "option_uid", "value_ts", "value_text", "team_uid", ...
     */
    private DataType resolveDataType(ElementTemplateConfig etc, DataElement de) {
        // prefer DataElement (immutable) for type inference
        if (de != null) {
            return switch (de.getValueType()) {
                case Number, Integer, IntegerPositive, IntegerNegative,
                     IntegerZeroOrPositive, Percentage,
                     UnitInterval -> DataType.NUMERIC;
                case Boolean, TrueOnly -> DataType.BOOLEAN;
                case SelectMulti -> DataType.OPTION;
                case Date, DateTime, Time -> DataType.TIMESTAMP;
                case OrganisationUnit, SelectOne, Activity, Team -> DataType.UID;
                default -> DataType.TEXT;
            };
        }

        // fallback to template hints if DataElement absent
        if (Boolean.TRUE.equals(etc.getIsMulti())) return DataType.OPTION;
        if (Boolean.TRUE.equals(etc.getIsReference())) return DataType.UID;
        return DataType.TEXT;
    }

    private static DataType mapValueTypeToDataType(ValueType vt) {
        if (vt == null) return DataType.TEXT;
        return switch (vt) {
            case Number, Integer, IntegerPositive, IntegerNegative,
                 IntegerZeroOrPositive, Percentage,
                 UnitInterval -> DataType.NUMERIC;
            case Boolean -> DataType.BOOLEAN;
            case SelectMulti -> DataType.OPTION;
            case Date, DateTime -> DataType.TIMESTAMP;
            case Team, OrganisationUnit,
                 Activity, SelectOne -> DataType.UID;
            default -> DataType.TEXT;
        };
    }
}
