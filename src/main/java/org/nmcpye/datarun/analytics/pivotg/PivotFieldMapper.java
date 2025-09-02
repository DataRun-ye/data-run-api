package org.nmcpye.datarun.analytics.pivotg;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jooq.tables.records.DataElementRecord;
import org.nmcpye.datarun.jooq.tables.records.ElementTemplateConfigRecord;

import java.util.*;

import static java.util.Map.entry;

/**
 * @author Hamza Assada
 * @since 26/08/2025
 */
@Mapper(componentModel = "spring")
public interface PivotFieldMapper {
    Map<ValueType, Set<Aggregation>> DEFAULT_ALLOWED = Map.ofEntries(
        entry(ValueType.Number, Set.of(Aggregation.SUM, Aggregation.AVG, Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
        entry(ValueType.Integer, Set.of(Aggregation.SUM, Aggregation.AVG, Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
        entry(ValueType.Percentage, Set.of(Aggregation.AVG, Aggregation.COUNT)),
        entry(ValueType.Boolean, Set.of(Aggregation.COUNT, Aggregation.SUM, Aggregation.COUNT_DISTINCT)),
        entry(ValueType.Date, Set.of(Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
        entry(ValueType.DateTime, Set.of(Aggregation.MIN, Aggregation.MAX, Aggregation.COUNT)),
        entry(ValueType.Text, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
        entry(ValueType.SelectOne, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
        entry(ValueType.SelectMulti, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
        entry(ValueType.Team, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
        entry(ValueType.OrganisationUnit, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
        entry(ValueType.Activity, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT)),
        entry(ValueType.Entity, Set.of(Aggregation.COUNT, Aggregation.COUNT_DISTINCT))
    );

    // --- Global Mode Mapping ---
    @Mapping(target = "id", expression = "java(\"de:\" + de.getUid())") // Prefix with "de:" to namespace
    @Mapping(target = "label", source = "name")
    @Mapping(target = "category", constant = "DYNAMIC_MEASURE")
    @Mapping(target = "dataType", source = "type", qualifiedByName = "mapValueType")
    @Mapping(target = "factColumn", source = "type", qualifiedByName = "mapFactColumn")
    @Mapping(target = "aggregationModes", source = "type", qualifiedByName = "mapAllowedAggregations")
    @Mapping(target = "deAggregationType", source = "type", qualifiedByName = "defaultAggregationType")
    @Mapping(target = "templateModeOnly", constant = "false")
    PivotFieldDto toPivotFieldDTO(DataElementRecord de);

    List<PivotFieldDto> toPivotFieldDTOs(List<DataElementRecord> dataElements);

    // --- Template Mode Mapping (example, may need more sources) ---
    // This mapping would be more complex, potentially needing a custom method
    // to join with data_element to get the UID and type information.
    @Mapping(target = "id", expression = "java(\"etc:\" + config.getUid())") // Prefix with "etc:" to namespace
    @Mapping(target = "label", source = "name")
    @Mapping(target = "category", constant = "DYNAMIC_MEASURE")
    @Mapping(target = "dataType", source = "valueType", qualifiedByName = "mapValueType")
    @Mapping(target = "factColumn", source = "valueType", qualifiedByName = "mapFactColumn")
    @Mapping(target = "aggregationModes", source = "valueType", qualifiedByName = "mapAllowedAggregations")
    @Mapping(target = "deAggregationType", source = "valueType", qualifiedByName = "defaultAggregationType")
    @Mapping(target = "templateModeOnly", constant = "true")
    PivotFieldDto toPivotFieldDTOsFromConfigs(ElementTemplateConfigRecord config);

    List<PivotFieldDto> toPivotFieldDTOsFromConfigs(List<ElementTemplateConfigRecord> configs);


    // --- Custom Mapping Logic ---
    @Named("mapValueType")
    default FieldValueType mapFieldValueType(String elementType) {
        var type = ValueType.valueOf(elementType);
        return switch (type) {
            case Number, Integer, Percentage,
                 UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> FieldValueType.NUMERIC;
            case Date, DateTime, Time -> FieldValueType.TIMESTAMP;
            case Boolean, TrueOnly -> FieldValueType.BOOLEAN;
            case SelectMulti -> FieldValueType.MULTI_SELECT_OPTION;
            case OrganisationUnit,
                 Team, Activity, Entity,
                 SelectOne -> FieldValueType.REFERENCE;
            default -> FieldValueType.TEXT;

        };
    }

    @Named("mapAllowedAggregations")
    default Set<Aggregation> mapAllowedAggregationTypes(String elementType) {
        var type = ValueType.valueOf(elementType);
        return Optional.ofNullable(DEFAULT_ALLOWED.get(type)).orElse(Collections.emptySet());
    }

    @Named("mapFactColumn")
    default String mapFactColumn(String elementType) {
        final FieldValueType ft = mapFieldValueType(elementType);
        return switch (ft) {
            case NUMERIC -> "value_num";
            case BOOLEAN -> "value_bool";
            case MULTI_SELECT_OPTION -> "option_id";
            case REFERENCE -> "value_ref";
            case TIMESTAMP -> "value_ts";
            default -> "value_text";
        };
    }

    @Named("defaultAggregationType")
    default Aggregation mapAggregationType(String elementType) {
        final FieldValueType ft = mapFieldValueType(elementType);
        return switch (ft) {
            case NUMERIC -> Aggregation.SUM;
            case BOOLEAN -> Aggregation.SUM_TRUE;
            case MULTI_SELECT_OPTION -> Aggregation.COUNT_DISTINCT;
            case REFERENCE -> Aggregation.COUNT_DISTINCT;
            case TIMESTAMP -> Aggregation.MAX;
            default -> Aggregation.COUNT;
        };
    }
}
