//package org.nmcpye.datarun.analytics.pivot;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.jooq.Condition;
//import org.jooq.DSLContext;
//import org.jooq.Field;
//import org.jooq.impl.DSL;
//import org.nmcpye.datarun.analytics.pivot.dto.MeasureRequest;
//import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
//import org.nmcpye.datarun.analytics.pivot.exception.InvalidMeasureException;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jooq.Tables;
//import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
//import org.nmcpye.datarun.jpa.dataelement.DataElement;
//import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
//import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.Locale;
//import java.util.Optional;
//import java.util.Set;
//
//import static org.nmcpye.datarun.jooq.tables.PivotGridFacts.PIVOT_GRID_FACTS;
//
///**
// * Template-mode-first MeasureValidationService.
// * <p>
// * Notes:
// * - Prefers template-scoped filter using etc_uid if the metadata DTO corresponds to that template.
// * - Falls back to global element_id matching if needed.
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MeasureValidationServiceImplOld implements MeasureValidationService {
//
//    private final PivotMetadataService pivotMetadataService;
//    private final PivotFieldJooqMapper fieldMapper;
//    private final ElementTemplateConfigRepository etcRepository;
//    private final DataElementRepository dataElementRepository;
//    private final DSLContext dsl;
//    private final AllowedAggregationsResolver aggrResolver;
//
//    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public ValidatedMeasure validate(MeasureRequest req, String templateUid, String templateVersionUid) throws InvalidMeasureException {
//        if (req == null) throw new InvalidMeasureException("MeasureRequest is required");
//        if (req.getElementIdOrUid() == null || req.getElementIdOrUid().isBlank())
//            throw new InvalidMeasureException("elementIdOrUid is required");
//
//        String clientId = req.getElementIdOrUid().trim();
//
//        // 1) Resolve via metadata service (template-first)
//        Optional<PivotFieldDto> maybeDto = pivotMetadataService.resolveFieldByUidOrId(clientId, templateUid, templateVersionUid);
//
//        PivotFieldDto dto = null;
//        String effectiveMode = "TEMPLATE";
//
//        if (maybeDto.isPresent()) {
//            dto = maybeDto.get();
//        } else {
//            // Fallback: try to load a DataElement by uid (global)
//            // Accept values like "de:UID" or just "UID"
//            String maybeDeUid = clientId.startsWith("de:") ? clientId.substring("de:".length()) : clientId;
//            Optional<DataElement> deOpt = dataElementRepository.findByUid(maybeDeUid);
//            if (deOpt.isPresent()) {
//                DataElement de = deOpt.get();
//                dto = PivotFieldDto.builder()
//                    .id("de:" + de.getUid())
//                    .label(de.getName())
//                    .category("FORM_MEASURE")
//                    .dataType(mapFactColumn(de.getValueType()))
//                    .aggregationModes(null) // unknown; rely on AllowedAggregationsResolver elsewhere if needed
//                    .templateModeOnly(false)
//                    .source("data_element")
//                    .build();
//                effectiveMode = "GLOBAL";
//            } else {
//                throw new InvalidMeasureException("Element not found in template or global catalog: " + clientId);
//            }
//        }
//
//        // 2) Aggregation validation
//        String requestedAgg = req.getAggregation();
//        if (requestedAgg == null || requestedAgg.isBlank()) requestedAgg = "COUNT";
//        requestedAgg = requestedAgg.toUpperCase(Locale.ROOT);
//
//        Set<String> allowed = dto.aggregationModes();
//        if (allowed != null && !allowed.isEmpty() && !allowed.contains(requestedAgg)) {
//            throw new InvalidMeasureException("Aggregation " + requestedAgg + " not allowed for field " + clientId);
//        }
//
//        // 3) Build element predicate (UID-native) and record uids
//        Condition elementPredicate = DSL.noCondition();
//        String elementTemplateConfigUid = null;
//        String elementUid = null;
//
//        // dto.id() is expected to be "etc:<uid>" for template fields or "de:<uid>" for global DEs
//        String dtoId = dto.id();
//        if (dtoId != null && dtoId.startsWith("etc:")) {
//            elementTemplateConfigUid = dtoId.substring("etc:".length());
//            elementPredicate = PIVOT_GRID_FACTS.ETC_UID.eq(elementTemplateConfigUid);
//        } else if (dtoId != null && dtoId.startsWith("de:")) {
//            elementUid = dtoId.substring("de:".length());
//            elementPredicate = PIVOT_GRID_FACTS.DE_UID.eq(elementUid);
//        } else {
//            // resilient parsing: treat plain 11-char as a UID, prefer template resolution
//            if (dtoId != null && dtoId.length() == 11 && dtoId.chars().allMatch(Character::isLetterOrDigit)) {
//                // try template first:
//                Optional<ElementTemplateConfig> etcOpt =
//                    etcRepository.findByUid(dtoId);
//                if (etcOpt.isPresent()) {
//                    elementTemplateConfigUid = dtoId;
//                    elementPredicate = PIVOT_GRID_FACTS.ETC_UID.eq(elementTemplateConfigUid);
//                } else {
//                    // fallback to data element by uid
//                    Optional<DataElement> deOpt = dataElementRepository.findByUid(dtoId);
//                    if (deOpt.isPresent()) {
//                        elementUid = dtoId;
//                        elementPredicate = PIVOT_GRID_FACTS.DE_UID.eq(elementUid);
//                        effectiveMode = "GLOBAL";
//                    } else {
//                        throw new InvalidMeasureException("Cannot resolve element uid: " + dtoId);
//                    }
//                }
//            } else {
//                // best effort fallback to element_id eq
//                elementPredicate = DSL.noCondition();
//            }
//        }
//
//        // Option scoping if present in request
//        String optionUid = req.getOptionId();
//        if (optionUid != null && !optionUid.isBlank()) {
//            elementPredicate = elementPredicate.and(PIVOT_GRID_FACTS.OPTION_UID.eq(optionUid));
//        }
//
//        // 4) Determine targetField using fieldMapper (typed jOOQ Field)
//        Field<?> targetField = fieldMapper.toJooqField(dto.dataType());
//
//        // 5) Interpret aggregation enum
//        ValidatedMeasure.MeasureAggregation aggEnum;
//        try {
//            aggEnum = ValidatedMeasure.MeasureAggregation.valueOf(requestedAgg);
//        } catch (IllegalArgumentException iae) {
//            throw new InvalidMeasureException("Unsupported aggregation: " + requestedAgg);
//        }
//
//        // 6) alias fallback
//        String alias = req.getAlias();
//        if (alias == null || alias.isBlank()) {
//            String base = dto.id() != null ? dto.id().replaceAll("[:]", "_") : "measure";
//            alias = base + "_" + requestedAgg.toLowerCase(Locale.ROOT);
//        }
//
//        return new ValidatedMeasure(
//            elementUid,
//            elementTemplateConfigUid,
//            aggEnum,
//            targetField,
//            elementPredicate,
//            alias,
//            Boolean.TRUE.equals(req.getDistinct()),
//            optionUid,
//            effectiveMode
//        );
//    }
//
//    /**
//     * Map ValueType (data element type) to pivot fact column exposed in PivotFieldDto / used for column mapping.
//     * <p>
//     * Examples:
//     * <pre>
//     * - Number, Integer -> "value_num"
//     * - Boolean -> "value_bool"
//     * - SelectMulti -> "option_uid" (multi produces one row per option)
//     * - Date/DateTime -> "value_ts"
//     * - SelectOne/OrgUnit/Team/Activity/Domain entity -> "value_ref_uid"
//     * - Default -> "value_text"
//     * </pre>
//     */
//    private static String mapFactColumn(ValueType vt) {
//        if (vt == null) return "value_text";
//        return switch (vt) {
//            case Number, Integer, IntegerPositive, IntegerNegative, IntegerZeroOrPositive, Percentage, UnitInterval ->
//                "value_num";
//            case Boolean -> "value_bool";
//            case SelectOne, SelectMulti -> "option_id";
//            case Date, DateTime -> "value_ts";
//            default -> "value_text";
//        };
//    }
//}
//
