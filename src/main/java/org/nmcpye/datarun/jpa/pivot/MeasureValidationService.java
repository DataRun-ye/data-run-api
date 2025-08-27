//package org.nmcpye.datarun.jpa.pivot;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.jooq.Condition;
//import org.jooq.Field;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
//import org.nmcpye.datarun.jpa.dataelement.DataElement;
//import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.Set;
//
//import static org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS;
//
///**
// * Validates requested measures and maps them to facts columns + element filters.
// * <p>
// * - ElementTemplateRepository provides findByDataElementId(String) -> Optional<ElementTemplateConfig>
// * - PivotGridFacts fields include VALUE_NUM, VALUE_BOOL, OPTION_ID, VALUE_TEXT, VALUE_TS and ELEMENT_ID
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MeasureValidationService {
//
//    private final ElementTemplateConfigRepository elementTemplateRepository;
//    private final DataElementRepository elementRepository;
//    private final PivotGridFacts PG = PIVOT_GRID_FACTS;
//
//    // Simple mapping of ValueType → allowed aggregations
//    private static final Set<PivotMeasureRequest.Aggregation> NUMERIC_AGGS = Set.of(
//            PivotMeasureRequest.Aggregation.SUM,
//            PivotMeasureRequest.Aggregation.AVG,
//            PivotMeasureRequest.Aggregation.MIN,
//            PivotMeasureRequest.Aggregation.MAX,
//            PivotMeasureRequest.Aggregation.COUNT,
//            PivotMeasureRequest.Aggregation.COUNT_DISTINCT
//    );
//
//    private static final Set<PivotMeasureRequest.Aggregation> BOOLEAN_AGGS = Set.of(
//            PivotMeasureRequest.Aggregation.SUM_TRUE,
//            PivotMeasureRequest.Aggregation.COUNT,
//            PivotMeasureRequest.Aggregation.COUNT_DISTINCT
//    );
//
//    private static final Set<PivotMeasureRequest.Aggregation> TEXT_AGGS = Set.of(
//            PivotMeasureRequest.Aggregation.COUNT,
//            PivotMeasureRequest.Aggregation.COUNT_DISTINCT
//    );
//
//    private static final Set<PivotMeasureRequest.Aggregation> DATE_AGGS = Set.of(
//            PivotMeasureRequest.Aggregation.MIN,
//            PivotMeasureRequest.Aggregation.MAX,
//            PivotMeasureRequest.Aggregation.COUNT,
//            PivotMeasureRequest.Aggregation.COUNT_DISTINCT
//    );
//
//    private static final Set<PivotMeasureRequest.Aggregation> OPTION_AGGS = Set.of(
//            PivotMeasureRequest.Aggregation.COUNT,
//            PivotMeasureRequest.Aggregation.COUNT_DISTINCT
//    );
//
//    /**
//     * Validate a single requested measure and return a ValidatedMeasure
//     */
//    public ValidatedMeasure validate(PivotMeasureRequest req) {
//        if (req == null) throw new IllegalArgumentException("measure request is required");
//
//        String elementId = req.getElementId();
//        var agg = req.getAggregation();
//        if (elementId == null || elementId.isBlank()) {
//            throw new IllegalArgumentException("elementId is required");
//        }
//        if (agg == null) {
//            throw new IllegalArgumentException("aggregation is required");
//        }
//
//        // Lookup element config (ElementTemplateConfig) by the canonical data element id
//        Optional<DataElement> tfOpt = elementRepository.findByUid(elementId);
//        if (tfOpt.isEmpty()) {
//            throw new IllegalArgumentException("Unknown element: " + elementId);
//        }
//        DataElement tf = tfOpt.get();
//
//        ValueType vt = tf.getType();
//        if (vt == null) {
//            throw new IllegalArgumentException("Element has no dataType configured: " + elementId);
//        }
//
//        // choose target column and validate aggregation against type
//        Field<?> targetField = resolveTargetFieldForValueType(vt);
//        boolean allowed = isAggregationAllowedForValueType(agg, vt);
//        if (!allowed) {
//            throw new IllegalArgumentException("Aggregation " + agg + " not allowed for element " + elementId + " of type " + vt);
//        }
//
//        // build element filter: facts rows with the element_id
//        Condition elementFilter = PG.ELEMENT_ID.eq(elementId);
//
//        // For select-one/multi, if optionId is provided, add that filter on top of elementFilter in the PivotQueryBuilder
//        boolean distinct = Boolean.TRUE.equals(req.getDistinct());
//
//        String alias = req.getAlias();
//        if (alias == null || alias.isBlank()) alias = buildAlias(elementId, agg, req.getOptionId());
//
//        return new ValidatedMeasure(elementId, agg, alias, targetField, elementFilter, distinct, req.getOptionId());
//    }
//
//    private String buildAlias(String elementId, PivotMeasureRequest.Aggregation agg, String optionId) {
//        String a = elementId + "_" + agg.name().toLowerCase();
//        if (optionId != null) a = a + "_" + optionId;
//        return a;
//    }
//
//    private Field<?> resolveTargetFieldForValueType(ValueType vt) {
//        // Use generated jOOQ fields if available; else fallback to DSL.field(...) with the expected java type
//        return switch (vt) {
//            case Number, Integer, IntegerPositive, IntegerNegative,
//                 IntegerZeroOrPositive, Percentage, UnitInterval -> (Field<BigDecimal>) PG.VALUE_NUM;
//            case Boolean, TrueOnly -> (Field<Boolean>) PG.VALUE_BOOL;
//            case SelectMulti -> (Field<String>) PG.OPTION_ID;
//            case SelectOne, OrganisationUnit, Team, Activity -> PG.VALUE_REF;
//            case Date, DateTime, Time -> (Field<LocalDateTime>) PG.VALUE_TS;
//            default -> (Field<String>) PG.VALUE_TEXT;
//        };
//    }
//
//    private boolean isAggregationAllowedForValueType(PivotMeasureRequest.Aggregation agg, ValueType vt) {
//        return switch (vt) {
//            case Number, Integer, IntegerPositive, IntegerNegative, IntegerZeroOrPositive, Percentage, UnitInterval ->
//                    NUMERIC_AGGS.contains(agg);
//            case Boolean -> BOOLEAN_AGGS.contains(agg);
//            case SelectOne, SelectMulti -> OPTION_AGGS.contains(agg);
//            case Date, DateTime -> DATE_AGGS.contains(agg);
//            case Text, TrueOnly -> TEXT_AGGS.contains(agg);
//            default -> false;
//        };
//    }
//
//    /**
//     * Helper to check whether a generated field exists on the jOOQ table (returns Optional).
//     */
//    private <T> Optional<Field<T>> safeField(PivotGridFacts table, String name, Class<T> type) {
//        try {
//            // This uses the generated jOOQ API: table.field(...) with Name or String
//            @SuppressWarnings("unchecked")
//            Field<T> f = (Field<T>) table.field(name);
//            return Optional.ofNullable(f);
//        } catch (Throwable t) {
//            log.debug("field {} not available on PivotGridFacts: {}", name, t.getMessage());
//            return Optional.empty();
//        }
//    }
//}
