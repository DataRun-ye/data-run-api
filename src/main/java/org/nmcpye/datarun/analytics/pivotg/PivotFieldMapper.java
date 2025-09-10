package org.nmcpye.datarun.analytics.pivotg;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
import org.nmcpye.datarun.analytics.pivot.dto.DataType;
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
    @Mapping(target = "extras", source = "type", qualifiedByName = "mapExtras")
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
    @Mapping(target = "extras", source = "valueType", qualifiedByName = "mapExtras")
    PivotFieldDto toPivotFieldDTOsFromConfigs(ElementTemplateConfigRecord config);

    List<PivotFieldDto> toPivotFieldDTOsFromConfigs(List<ElementTemplateConfigRecord> configs);


    // --- Custom Mapping Logic ---
    // the type, no category here, category and Multi is a different metas provided through extra
    @Named("mapValueType")
    default DataType mapFieldValueType(String elementType) {
        var type = ValueType.valueOf(elementType);
        return switch (type) {
            case Number, Integer, Percentage,
                 UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> DataType.NUMERIC;
            case Date, DateTime, Time -> DataType.TIMESTAMP;
            case Boolean, TrueOnly -> DataType.BOOLEAN;
            case SelectMulti, SelectOne -> DataType.OPTION;
            case OrganisationUnit,
                 Team, Activity, Entity -> DataType.UID;
            default -> DataType.TEXT;

        };
    }

    @Named("mapAllowedAggregations")
    default Set<Aggregation> mapAllowedAggregationTypes(String elementType) {
        var type = ValueType.valueOf(elementType);
        return Optional.ofNullable(DEFAULT_ALLOWED.get(type)).orElse(Collections.emptySet());
    }

    @Named("mapFactColumn")
    default String mapFactColumn(String elementType) {
        var type = ValueType.valueOf(elementType);
        return switch (type) {
            case Number, Integer, Percentage,
                 UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> "value_num";
            case Boolean, TrueOnly -> "value_bool";
            case SelectMulti -> "option_id";
            case OrganisationUnit,
                 Team, Activity, Entity, SelectOne -> "value_ref_uid";
            case Date, DateTime, Time -> "value_ts";
            default -> "value_text";
        };
    }

    @Named("mapExtras")
    default Map<String, Object> mapExtras(String elementType) {
        var type = ValueType.valueOf(elementType);

        Map<String, Object> extras = new HashMap<>();
        extras.put("isMulti", type == ValueType.SelectMulti);
        extras.put("isReference", Boolean.TRUE.equals(type.isReference()));
        return extras;
    }
}
