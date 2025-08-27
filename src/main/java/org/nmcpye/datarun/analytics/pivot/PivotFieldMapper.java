//package org.nmcpye.datarun.analytics.pivot.service;
//
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//import org.mapstruct.Named;
//import org.nmcpye.datarun.analytics.pivot.dto.FieldValueType;
//import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDTO;
//import org.nmcpye.datarun.datatemplateelement.AggregationType;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jooq.tables.records.DataElementRecord;
//import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
//
//import java.util.*;
//
//import static java.util.Map.entry;
//
///**
// * @author Hamza Assada
// * @since 26/08/2025
// */
//@Mapper(componentModel = "spring")
//public interface PivotFieldMapper {
//    Map<ValueType, Set<Aggregation>> DEFAULT_ALLOWED = Map.ofEntries(
//        entry(ValueType.Number, Set.of(Aggregation.SUM, Aggregation.AVG, Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//        entry(ValueType.Integer, Set.of(Aggregation.SUM, Aggregation.AVG, Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//        entry(ValueType.Percentage, Set.of(Aggregation.AVG, Aggregation.COUNT)),
//        entry(ValueType.Boolean, Set.of(Aggregation.COUNT, Aggregation.SUM, Aggregation.COUNT_DISTINCT)),
//        entry(ValueType.Date, Set.of(Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//        entry(ValueType.DateTime, Set.of(Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
//        entry(ValueType.Text, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//        entry(ValueType.SelectOne, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//        entry(ValueType.SelectMulti, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//        entry(ValueType.Team, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//        entry(ValueType.OrganisationUnit, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//        entry(ValueType.Activity, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
//        entry(ValueType.Entity, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
//    );
//
//    // --- Global Mode Mapping ---
//    @Mapping(target = "id", expression = "java(\"de:\" + de.getUid())") // Prefix with "de:" to namespace
//    @Mapping(target = "label", source = "name")
//    @Mapping(target = "category", constant = "DYNAMIC_MEASURE")
//    @Mapping(target = "dataType", source = "type", qualifiedByName = "mapValueType")
//    @Mapping(target = "factColumn", source = "type", qualifiedByName = "mapFactColumn")
//    @Mapping(target = "aggregationModes", source = "type", qualifiedByName = "mapAllowedAggregations")
//    @Mapping(target = "defaultAggregation", source = "type", qualifiedByName = "defaultAggregationType")
//    @Mapping(target = "templateModeOnly", constant = "false")
//    PivotFieldDTO toPivotFieldDTO(DataElementRecord de);
//
//    List<PivotFieldDTO> toPivotFieldDTOs(List<DataElementRecord> dataElements);
//
//    // --- Template Mode Mapping (example, may need more sources) ---
//    // This mapping would be more complex, potentially needing a custom method
//    // to join with data_element to get the UID and type information.
//    // List<PivotFieldDTO> toPivotFieldDTOsFromConfigs(List<ElementTemplateConfigRecord> configs);
//
//
//    // --- Custom Mapping Logic ---
//    @Named("mapValueType")
//    default FieldValueType mapFieldValueType(String elementType) {
//        final ValueType dataType = ValueType.valueOf(elementType);
//        return switch (dataType) {
//            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
//                 IntegerNegative, IntegerZeroOrPositive -> FieldValueType.NUMERIC;
//            case Date, DateTime, Time -> FieldValueType.TIMESTAMP;
//            case Boolean, TrueOnly -> FieldValueType.BOOLEAN;
//            case SelectMulti -> FieldValueType.OPTION;
//            case OrganisationUnit, Team, Activity, Entity, SelectOne -> FieldValueType.REFERENCE;
//            default -> FieldValueType.TEXT;
//
//        };
//    }
//
//    @Named("mapAllowedAggregations")
//    default Set<Aggregation> mapAllowedAggregationTypes(String elementType) {
//        final ValueType dataType = ValueType.valueOf(elementType);
//        return Optional.ofNullable(DEFAULT_ALLOWED.get(dataType)).orElse(Collections.emptySet());
//    }
//
//    @Named("mapFactColumn")
//    default String mapFactColumn(String elementType) {
//        final FieldValueType ft = mapFieldValueType(elementType);
//        return switch (ft) {
//            case NUMERIC -> "value_num";
//            case BOOLEAN -> "value_bool";
//            case OPTION -> "option_id";
//            case REFERENCE -> "value_ref";
//            case TIMESTAMP -> "value_ts";
//            default -> "value_text";
//        };
//    }
//
//    @Named("defaultAggregationType")
//    default AggregationType mapAggregationType(String elementType) {
//        final FieldValueType ft = mapFieldValueType(elementType);
//        return switch (ft) {
//            case NUMERIC -> AggregationType.SUM;
//            case BOOLEAN -> AggregationType.PERCENT_TRUE;
//            case OPTION -> AggregationType.COUNT_DISTINCT;
//            case REFERENCE -> AggregationType.COUNT_DISTINCT;
//            case TIMESTAMP -> AggregationType.MAX;
//            default -> AggregationType.COUNT;
//        };
//    }
//
//    // Additional mappers for allowed aggregations etc. would go here.
//}
