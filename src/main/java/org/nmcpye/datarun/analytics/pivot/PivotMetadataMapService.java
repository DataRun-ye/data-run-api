//package org.nmcpye.datarun.analytics.pivot;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
//import org.nmcpye.datarun.analytics.pivot.dto.PivotMetadataResponse;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.dataelement.DataElement;
//import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
//import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Template-mode-first metadata service.
// * <p>
// * Caching: cache results per templateId+versionId. Eviction should be handled by callers
// * when a template is published/updated.
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PivotMetadataMapService implements PivotMetadataService {
//
//    private final ElementTemplateConfigRepository etcRepository;
//    private final DataElementRepository dataElementRepository;
//    private final AllowedAggregationsResolver aggrResolver;
//
//    private static final String CACHE_PREFIX = "pivot.metadata:template:";
//    final public static String PIVOT_CACHE_NAME = "pivot.metadata";
//
//    @Override
//    @Cacheable(cacheNames = PIVOT_CACHE_NAME, key = "T(java.lang.String).valueOf('" + CACHE_PREFIX + "').concat(#templateUid).concat(':').concat(#templateVersionUid)")
//    public PivotMetadataResponse getMetadataForTemplate(String templateUid, String templateVersionUid) {
//        Objects.requireNonNull(templateUid, "templateUid is required");
//        Objects.requireNonNull(templateVersionUid, "templateVersionUid is required");
//
//        // 1. load element_template_config rows for this template+version
//        List<ElementTemplateConfig> etcRows = etcRepository.findAllByTemplateUidAndTemplateVersionUid(templateUid, templateVersionUid);
//
//        // 2. gather DataElement uids and batch load them
//        Set<String> dataElementUids = etcRows.stream()
//            .map(ElementTemplateConfig::getDataElementUid)
//            .filter(Objects::nonNull).collect(Collectors.toSet());
//        Map<String, DataElement> dataElementMap;
//        if (!dataElementUids.isEmpty()) {
//            dataElementMap = dataElementRepository.findAllByUidIn(dataElementUids).stream()
//                .collect(Collectors.toMap(DataElement::getUid, de -> de));
//        } else {
//            dataElementMap = Collections.emptyMap();
//        }
//
//        // 3. map each ElementTemplateConfig -> PivotFieldDto
//        List<PivotFieldDto> measures = etcRows.stream().map(etc -> {
//            DataElement de = dataElementMap.get(etc.getDataElementUid());
//            ValueType vt = de != null ? de.getValueType() : null;
//
//            Map<String, Object> extras = new HashMap<>();
//            extras.put("isMulti", Boolean.TRUE.equals(etc.getIsMulti()));
//            extras.put("isReference", Boolean.TRUE.equals(etc.getIsReference()));
//            extras.put("referenceTable", etc.getReferenceTable());
//            extras.put("optionSetUid", etc.getOptionSetUid());
//            extras.put("isCategory", etc.getIsCategory());
//            extras.put("repeatPath", etc.getRepeatPath());
//            extras.put("categoryForRepeat", etc.getCategoryForRepeat());
//
//            PivotFieldDto dto = PivotFieldDto.builder()
//                .id("etc:" + etc.getDataElementUid()) // template-scoped id for client (could be improved to expose uid)
//                .label(Objects.toString(etc.getDisplayLabel(), etc.getName()))
//                .category("FORM_MEASURE")
//                .dataType(resolveDataType(etc, de))
//                .aggregationModes(aggrResolver.allowedFor(vt))
//                .templateModeOnly(true)
//                .source("element_template_config")
//                .extras(extras)
//                .build();
//
//            return dto;
//        }).collect(Collectors.toList());
//
//        // core dimensions (minimal set). For now, static set;
//        // we can populate more metadata from DB if desired
//        List<PivotFieldDto> coreDimensions = List.of(
//            PivotFieldDto.builder().id("team_uid").label("Team").category("CORE_DIMENSION")
//                .dataType("team_uid").aggregationModes(Set.of("COUNT")).templateModeOnly(false).source("system").build(),
//            PivotFieldDto.builder().id("org_unit_uid").label("Org Unit").category("CORE_DIMENSION")
//                .dataType("org_unit_uid").aggregationModes(Set.of("COUNT")).templateModeOnly(false).source("system").build(),
//            PivotFieldDto.builder().id("activity_uid").label("Activity").category("CORE_DIMENSION")
//                .dataType("activity_uid").aggregationModes(Set.of("COUNT")).templateModeOnly(false).source("system").build(),
//            PivotFieldDto.builder().id("submission_completed_at").label("Submission completed at").category("CORE_DIMENSION")
//                .dataType("submission_completed_at").aggregationModes(Set.of("MIN", "MAX")).templateModeOnly(false).source("system").build()
//        );
//
//        return PivotMetadataResponse.builder()
//            .coreDimensions(coreDimensions)
//            .formDimensions(List.of()) // for later (repeat paths etc.)
//            .measures(measures)
//            .hints(Map.of(
//                "mode", "template",
//                "templateUid", templateUid,
//                "templateVersionUid", templateVersionUid))
//            .build();
//    }
//
//    @Override
//    public Optional<PivotFieldDto> resolveFieldByUidOrId(String uidOrId,
//                                                         String templateId, String templateVersionId) {
//        // Prefer template-level lookup first
//        List<ElementTemplateConfig> etcRows = etcRepository.findAllByTemplateUidAndTemplateVersionUid(templateId, templateVersionId);
//        for (ElementTemplateConfig etc : etcRows) {
//            String etcClientId = "etc:" + etc.getUid(); // same id as in DTO generation
//            if (etcClientId.equals(uidOrId) || Objects.equals(etc.getUid(), uidOrId)) {
//                // build DTO for this single etc
//                DataElement de = null;
//                if (etc.getDataElementUid() != null) {
//                    de = dataElementRepository.findByUid(etc.getDataElementUid()).orElse(null);
//                }
//                ValueType vt = de != null ? de.getValueType() : null;
//                Map<String, Object> extras = new HashMap<>();
//                extras.put("isMulti", Boolean.TRUE.equals(etc.getIsMulti()));
//                extras.put("isReference", Boolean.TRUE.equals(etc.getIsReference()));
//                extras.put("referenceTable", etc.getReferenceTable());
//                extras.put("optionSetUid", etc.getOptionSetUid());
//                extras.put("isCategory", etc.getCategoryForRepeat() != null);
//                extras.put("repeatPath", etc.getRepeatPath());
//                extras.put("categoryElementUidForRepeat", etc.getCategoryForRepeat());
//
//                PivotFieldDto dto = PivotFieldDto.builder()
//                    .id(etcClientId)
//                    .label(Objects.toString(etc.getDisplayLabel(), etc.getName()))
//                    .category("FORM_MEASURE")
//                    .dataType(resolveDataType(etc, de))
//                    .aggregationModes(aggrResolver.allowedFor(vt))
//                    .templateModeOnly(true)
//                    .source("element_template_config")
//                    .extras(extras)
//                    .build();
//
//                return Optional.of(dto);
//            }
//        }
//
//        // Not found in template; optionally fallback to global DataElement
//        return Optional.empty();
//    }
//
//    /**
//     * Decide which pivot dataType to expose for a given template config and optional DataElement meta.
//     */
//    private String resolveDataType(ElementTemplateConfig etc, DataElement de) {
//        // If element_template_config marks as reference or option-set then use option/ref types
//        if (Boolean.TRUE.equals(etc.getIsMulti())) {
//            return "option_id";
//        }
//        if (Boolean.TRUE.equals(etc.getIsReference())) {
//            return "value_ref";
//        }
//        // fallback to DataElement's ValueType
//        if (de == null || de.getValueType() == null) return "value_text";
//        switch (de.getValueType()) {
//            case Number, Integer, IntegerPositive,
//                 IntegerNegative, IntegerZeroOrPositive,
//                 Percentage, UnitInterval:
//                return "value_num";
//            case Boolean:
//                return "value_bool";
//            case SelectOne, SelectMulti:
//                return "option_id";
//            case Date, DateTime:
//                return "value_ts";
//            case Text, TrueOnly, Time:
//            default:
//                return "value_text";
//        }
//    }
//}
