//package org.nmcpye.datarun.jpa.pivot;
//
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.datatemplateelement.AggregationType;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
//import org.springframework.stereotype.Service;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import static java.util.Map.entry;
//
///**
// * Validates MeasureRequest(s) against ElementTemplateConfig metadata and maps to backend column(s).
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@Service
//@RequiredArgsConstructor
//public class MeasureValidationServiceOld {
//
//    private final PivotMetadataService metadataService;
//
//    @Getter
//    @AllArgsConstructor
//    public static class ValidatedMeasure {
//        /**
//         * element id
//         */
//        private final String elementId;
//
//        /**
//         * final alias
//         */
//        private final String alias;
//
//        private final Aggregation aggregation;
//
//        /**
//         * target column in pivot_grid_facts (value_num/option_id/value_bool/value_text)
//         */
//        private final String column;
//        /**
//         * distinct flag for COUNT
//         */
//        private final boolean distinct;      //
//    }
//
//    /**
//     * base mapping: ValueType -> allowed aggregations (default heuristics)
//     */
//    private static final Map<ValueType, Set<Aggregation>> DEFAULT_ALLOWED = Map.ofEntries(
//            entry(ValueType.Number, Set.of(Aggregation.SUM, Aggregation.AVG, Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//            entry(ValueType.Integer, Set.of(Aggregation.SUM, Aggregation.AVG, Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//            entry(ValueType.Percentage, Set.of(Aggregation.AVG, Aggregation.COUNT)),
//            entry(ValueType.Boolean, Set.of(Aggregation.COUNT, Aggregation.SUM, Aggregation.COUNT_DISTINCT)),
//            entry(ValueType.Date, Set.of(Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//            entry(ValueType.DateTime, Set.of(Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//            entry(ValueType.Text, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//            entry(ValueType.SelectOne, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//            entry(ValueType.SelectMulti, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//            entry(ValueType.Team, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//            entry(ValueType.OrganisationUnit, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//            entry(ValueType.Activity, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//            entry(ValueType.Entity, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//    );
//
//    /**
//     * Validate a list of incoming MeasureRequest; returns validated measures.
//     */
//    public List<ValidatedMeasure> validateMeasures(String templateId, Integer versionNo, List<MeasureRequest> requested) {
//        if (requested == null || requested.isEmpty()) return List.of();
//        return requested.stream().map(r -> validateSingle(templateId, versionNo, r)).collect(Collectors.toList());
//    }
//
//    private ValidatedMeasure validateSingle(String templateId, Integer versionNo, MeasureRequest r) {
//        if (r == null) throw new IllegalArgumentException("MeasureRequest is null");
//        String elementId = r.getElementId();
//        ElementTemplateConfig cfg = metadataService.getField(templateId, versionNo, elementId)
//                .orElseThrow(() -> new IllegalArgumentException("Unknown elementId " + elementId + " for template " + templateId));
//
//        ValueType vt = cfg.getValueType();
//        Aggregation requested = r.getAggregation() == null ? Aggregation.COUNT : r.getAggregation();
//        boolean distinct = Boolean.TRUE.equals(r.getDistinct());
//
//        // Determine backend column
//        String column = switch (vt) {
//            case Number, Integer, Percentage -> "value_num";
//            case Boolean -> "value_bool";
//            case SelectOne -> "option_id";
//            case SelectMulti -> "option_id";
//            default -> "value_text";
//        };
//
//        // Allowed set
//        Set<Aggregation> allowed = new HashSet<>();
//        allowed.add(Aggregation.COUNT);
//        allowed.add(Aggregation.COUNT_DISTINCT);
//
//        // If configured as measure, allow numeric aggs / configured aggregations
//        if (Boolean.TRUE.equals(cfg.getIsMeasure())) {
//            allowed.addAll(DEFAULT_ALLOWED.getOrDefault(vt, Set.of(Aggregation.COUNT)));
//        } else {
//            // if aggregationType explicitly set (non-default) allow that aggregator
//            AggregationType aType = cfg.getAggregationType();
//            if (aType != null && aType != AggregationType.DEFAULT) {
//                Aggregation mapped = mapAggType(aType);
//                if (mapped != null) allowed.add(mapped);
//            } else {
//                // fallback heuristic - allow default set for the value type
//                allowed.addAll(DEFAULT_ALLOWED.getOrDefault(vt, Set.of(Aggregation.COUNT)));
//            }
//        }
//
//        if (!allowed.contains(requested)) {
//            throw new IllegalArgumentException("Aggregation " + requested + " not allowed for element " + elementId + " (type=" + vt + ")");
//        }
//
//        String alias = r.getAlias() != null ? r.getAlias() : elementId + "_" + requested.name().toLowerCase();
//        return new ValidatedMeasure(elementId, alias, requested, column, distinct);
//    }
//
//    private Aggregation mapAggType(AggregationType t) {
//        if (t == null) return null;
//        return switch (t) {
//            case SUM -> Aggregation.SUM;
//            case AVG -> Aggregation.AVG;
//            case MIN -> Aggregation.MIN;
//            case MAX -> Aggregation.MAX;
//            default -> Aggregation.COUNT;
//        };
//    }
//}
