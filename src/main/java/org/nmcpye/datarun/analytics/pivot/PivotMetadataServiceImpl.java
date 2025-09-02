package org.nmcpye.datarun.analytics.pivot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
import org.nmcpye.datarun.analytics.pivot.dto.FieldCategory;
import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.analytics.pivot.dto.PivotMetadataResponse;
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

/**
 * Implementation notes for PivotMetadataServiceImpl:
 * <p>
 * - Should load all ElementTemplateConfig rows for templateUid+templateVersionUid, batch-load their DataElement metadata,
 * and produce a list of PivotFieldDto objects:
 * <pre>
 * - PivotFieldDto.factColumn must map to the materialized view column (e.g., "etc_uid", "value_num", "option_uid", "value_ts", "team_uid").
 * - PivotFieldDto.dataType uses the "value_num"/"option_uid"/"value_ts"/"value_bool"/"value_text" nomenclature.
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
public class PivotMetadataServiceImpl implements PivotMetadataService {

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
    public PivotMetadataResponse getMetadataForTemplate(String templateUid, String templateVersionUid) {
        Objects.requireNonNull(templateUid, "templateUid is required");
        Objects.requireNonNull(templateVersionUid, "templateVersionUid is required");

        List<ElementTemplateConfig> etcRows = etcRepository.findAllByTemplateUidAndTemplateVersionUid(templateUid, templateVersionUid);

        // Batch load data elements (immutable metadata)
        Set<String> dataElementUids = etcRows.stream()
            .map(ElementTemplateConfig::getDataElementUid)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<String, DataElement> dataElementMap = dataElementUids.isEmpty()
            ? Collections.emptyMap()
            : dataElementRepository.findAllByUidIn(dataElementUids)
            .stream().collect(Collectors.toMap(DataElement::getUid, de -> de));

        // Build measure DTOs from template config (template-mode)
        List<PivotFieldDto> measures = etcRows.stream().map(etc -> {
            DataElement de = dataElementMap.get(etc.getDataElementUid());
            ValueType vt = de != null ? de.getValueType() : null;

            Map<String, Object> extras = new HashMap<>();
            extras.put("isMulti", Boolean.TRUE.equals(etc.getIsMulti()));
            extras.put("isReference", Boolean.TRUE.equals(etc.getIsReference()));
            extras.put("referenceTable", etc.getReferenceTable());
            extras.put("optionSetUid", etc.getOptionSetUid());
            extras.put("isCategory", Boolean.TRUE.equals(etc.getIsCategory()));
            extras.put("repeatPath", etc.getRepeatPath());
            extras.put("categoryForRepeat", etc.getCategoryForRepeat());

            // **dataType:** where to read the value for aggregation
            String dataType = resolveDataType(etc, de); // returns "value_num", "value_ts", "option_uid", etc.

            // **We separate two concepts:**
            //  - identity / predicate column — the MV column used to identify rows belonging to
            //  a logical element (e.g. etc_uid) or
            //  dimension (e.g. team_uid, etc.). This is the factColumn value in PivotFieldDto.
            //  - measure target / dataType — which MV value column stores
            //  the actual measurement value (value_num, value_text, value_bool,
            //  value_ts, option_uid, value_ref_uid). This remains dataType in PivotFieldDto.
            //
            //  ---
            //
            // FACT COLUMN: the MV column to use to identify & predicate rows for this configured field
            // For template-mode fields we always use the etc_uid MV column to filter/group.
            //
            // **Notes:**
            // pivotFieldDto.factColumn is always set for template-mode
            // fields to "etc_uid".
            //  - For core dimensions we set factColumn to their UID column (e.g. team_uid).
            //  - If we later add fields that are best looked up by de_uid instead
            //  of etc_uid, set factColumn accordingly at DTO time.
            //
            // Explicit factColumn removes heuristics in validation and
            String factColumn = "etc_uid"; // template-mode fields map to this

            Set<Aggregation> aggModes = aggrResolver.allowedFor(vt);

            return PivotFieldDto.builder()
                .id("etc:" + etc.getUid())
                .label(Objects.toString(etc.getDisplayLabel(), etc.getName()))
                .category(FieldCategory.DYNAMIC_MEASURE)
                .dataType(dataType)
                .factColumn(factColumn)
                .aggregationModes(aggModes)
                .templateModeOnly(true)
                .source("etc")
                .extras(extras)
                .build();
        }).collect(Collectors.toList());

        return PivotMetadataResponse.builder()
            .coreDimensions(getCoreDimensions())
            .formDimensions(List.of())
            .measures(measures)
            .hints(Map.of(
                "mode", "template",
                "templateUid", templateUid,
                "templateVersionUid", templateVersionUid))
            .build();
    }

    // Core dimensions (uid-native)
    private List<PivotFieldDto> getCoreDimensions() {
        return List.of(
            PivotFieldDto.builder()
                .id("team_uid")
                .label("Team")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType("team_uid")
                .factColumn("team_uid")
                .aggregationModes(aggrResolver.allowedFor(ValueType.Team))
                .templateModeOnly(false).source("system")
                .build(),

            PivotFieldDto.builder()
                .id("org_unit_uid")
                .label("Org Unit")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType("org_unit_uid")
                .factColumn("org_unit_uid")
                .aggregationModes(aggrResolver.allowedFor(ValueType.OrganisationUnit))
                .templateModeOnly(false)
                .source("system")
                .build(),

            PivotFieldDto.builder()
                .id("activity_uid")
                .label("Activity")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType("activity_uid")
                .factColumn("activity_uid")
                .aggregationModes(aggrResolver.allowedFor(ValueType.Activity))
                .templateModeOnly(false)
                .source("system")
                .build(),

            PivotFieldDto.builder()
                .id("submission_completed_at")
                .label("Submission completed at")
                .category(FieldCategory.CORE_DIMENSION)
                .dataType("submission_completed_at")
                .factColumn("submission_completed_at")
                .aggregationModes(Set.of(Aggregation.MIN, Aggregation.MAX))
                .templateModeOnly(false)
                .source("system")
                .build()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<PivotFieldDto> resolveFieldByUidOrId(String uidOrId, String templateUid, String templateVersionUid) {
        if (uidOrId == null) return Optional.empty();

        // template-level lookup
        List<ElementTemplateConfig> etcRows = etcRepository.findAllByTemplateUidAndTemplateVersionUid(templateUid, templateVersionUid);
        for (ElementTemplateConfig etc : etcRows) {
            String etcClientId = "etc:" + etc.getUid();
            if (etcClientId.equals(uidOrId) || Objects.equals(etc.getUid(), uidOrId)) {
                DataElement de = null;
                if (etc.getDataElementUid() != null) {
                    de = dataElementRepository.findByUid(etc.getDataElementUid()).orElse(null);
                }
                String dataType = resolveDataType(etc, de);

                Map<String, Object> extras = new HashMap<>();
                extras.put("isMulti", Boolean.TRUE.equals(etc.getIsMulti()));
                extras.put("isReference", Boolean.TRUE.equals(etc.getIsReference()));
                extras.put("optionSetUid", etc.getOptionSetUid());
                extras.put("isCategory", etc.getCategoryForRepeat() != null);
                extras.put("repeatPath", etc.getRepeatPath());
                extras.put("categoryForRepeat", etc.getCategoryForRepeat());

                PivotFieldDto dto = PivotFieldDto.builder()
                    .id(etcClientId)
                    .label(Objects.toString(etc.getDisplayLabel(), etc.getName()))
                    .category(FieldCategory.DYNAMIC_MEASURE)
                    .dataType(dataType)
                    .factColumn("etc_uid")
                    .aggregationModes(aggrResolver
                        .allowedFor(de != null ? de.getValueType() : null))
                    .templateModeOnly(true)
                    .source("etc")
                    .extras(extras)
                    .build();
                return Optional.of(dto);
            }
        }

        // Not a template field. Optionally fall back to global DataElement
        if (uidOrId.startsWith("de:")) {
            String deUid = uidOrId.substring("de:".length());
            DataElement de = dataElementRepository.findByUid(deUid).orElse(null);
            if (de != null) {
                Map<String, Object> extras = new HashMap<>();
                extras.put("isMulti", de.getValueType() == ValueType.SelectMulti);
                extras.put("isReference", de.getValueType().isSystemReferenceType());
                extras.put("optionSetUid", de.getOptionSet().getUid());

                PivotFieldDto dto = PivotFieldDto.builder()
                    .id("de:" + de.getUid())
                    .label(de.getName())
                    .category(FieldCategory.DYNAMIC_MEASURE)
                    .dataType(mapValueTypeToDataType(de.getValueType()))
                    .factColumn("de_uid")
                    .aggregationModes(aggrResolver.allowedFor(de.getValueType()))
                    .templateModeOnly(false)
                    .source("data_element")
                    .extras(extras)
                    .build();
                return Optional.of(dto);
            }
        }

        return Optional.empty();
    }

    /**
     * Decide which pivot dataType to expose for a given template config and optional DataElement meta.
     * <p>
     * This method returns one of the pivot dataType strings used across the analytics stack:
     * - "value_num", "value_bool", "option_uid", "value_ts", "value_text", "team_uid", ...
     */
    private String resolveDataType(ElementTemplateConfig etc, DataElement de) {
        // prefer DataElement (immutable) for type inference
        if (de != null) {
            return switch (de.getValueType()) {
                case Number, Integer, IntegerPositive, IntegerNegative,
                     IntegerZeroOrPositive, Percentage,
                     UnitInterval -> "value_num";
                case Boolean, TrueOnly -> "value_bool";
                case SelectOne, SelectMulti ->
                    // multi-select rows will populate option_uid; single select/reference will populate value_ref_uid
                    Boolean.TRUE.equals(etc.getIsMulti()) ? "option_uid" : "value_ref_uid";
                case Date, DateTime, Time -> "value_ts";
                default -> "value_text";
            };
        }

        // fallback to template hints if DataElement absent
        if (Boolean.TRUE.equals(etc.getIsMulti())) return "option_uid";
        if (Boolean.TRUE.equals(etc.getIsReference())) return "value_ref_uid";
        return "value_text";
    }

    private static String mapValueTypeToDataType(ValueType vt) {
        if (vt == null) return "value_text";
        return switch (vt) {
            case Number, Integer, IntegerPositive, IntegerNegative,
                 IntegerZeroOrPositive, Percentage, UnitInterval -> "value_num";
            case Boolean -> "value_bool";
            case SelectMulti -> "option_uid";
            case Date, DateTime -> "value_ts";
            case Team, OrganisationUnit, Activity, SelectOne -> "value_ref_uid";
            default -> "value_text";
        };
    }
}
