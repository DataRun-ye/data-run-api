package org.nmcpye.datarun.jpa.pivot.query;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jooq.tables.ElementDataValue;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.jooq.impl.DSL.noCondition;
import static org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS;

/**
 * PivotRegistry: central registry mapping API ids -> PivotableFieldMapping
 *
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
@Component
@RequiredArgsConstructor
public class PivotRegistry {

    private final Map<String, PivotableFieldMapping> mappings = new ConcurrentHashMap<>();

    @PostConstruct
    private void initialize() {
        // --- Static Dimension Mappings ---
        mappings.put("assignment.team_id", new PivotableFieldMapping("assignment.team_id",
            PIVOT_GRID_FACTS.TEAM_ID, null, PivotDataType.TEXT, noCondition()));

        mappings.put("assignment.org_unit_id", new PivotableFieldMapping("assignment.org_unit_id",
            PIVOT_GRID_FACTS.ORG_UNIT_ID, null, PivotDataType.TEXT, noCondition()));

        mappings.put("element.id", new PivotableFieldMapping("element.id",
            PIVOT_GRID_FACTS.ELEMENT_ID, null, PivotDataType.TEXT, noCondition()));

        // --- Static Measure Mappings ---
        mappings.put("submission.count", new PivotableFieldMapping("submission.count",
            null, PIVOT_GRID_FACTS.SUBMISSION_ID, PivotDataType.NUMERIC, noCondition()));
    }

    public Optional<PivotableFieldMapping> findMapping(String id) {
        // First, check static mappings.
        if (mappings.containsKey(id)) {
            return Optional.of(mappings.get(id));
        }

        // If not found, check if it's a dynamic element-based measure.
        if (id.startsWith("element.measure.")) {
            return Optional.of(mappings.computeIfAbsent(id, this::createMappingForElementMeasure));
        }

        return Optional.empty();
    }

    /**
     * Dynamically creates a mapping for a measure based on a specific element.
     * Example ID: "element.measure.01H8X... (numeric type)"
     */
    private PivotableFieldMapping createMappingForElementMeasure(String fullElementId) {
        String elementId = fullElementId.replace("element.measure.", "");

        // This measure is valid ONLY for rows matching this element ID.
        Condition condition = PIVOT_GRID_FACTS.ELEMENT_ID.eq(elementId);

        // Here we assume numeric aggregation. A more advanced version could look up
        // the element's value_type to choose between VALUE_NUM, VALUE_TS, etc.
        return new PivotableFieldMapping(
            fullElementId,
            null, // This is a measure, not a dimension.
            PIVOT_GRID_FACTS.VALUE_NUM, // The field to aggregate.
            PivotDataType.NUMERIC,
            condition // The crucial condition for the FILTER clause.
        );
    }

    private Field<?> getFieldForDataType(PivotDataType dataType, ElementDataValue elementDataValue) {
        return switch (dataType) {
            case NUMERIC -> elementDataValue.VALUE_NUM;
            case BOOLEAN -> elementDataValue.VALUE_BOOL;
            // Add cases for DATE, DATETIME when those columns exist
            default -> elementDataValue.VALUE_TEXT;
        };
    }

    /**
     * map specific domain enum to pivot enum
     *
     * @param valueType domain value type
     * @return pivot value type
     */
    private PivotDataType mapValueTypeToPivotType(ValueType valueType) {
        return switch (valueType) {
            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> PivotDataType.NUMERIC;
            case Boolean, TrueOnly -> PivotDataType.BOOLEAN;
            case Date -> PivotDataType.DATE;
            case DateTime -> PivotDataType.DATETIME;
            default -> PivotDataType.TEXT;
        };
    }
}
//@Component
//@RequiredArgsConstructor
//public class PivotRegistry {
//
//    private final DataElementRepository dataElementRepository;
//
//    // To check if an element is in a repeat
/// /    private final FormDataElementConfRepository formConfRepository;
//
//    /**
//     * Cache for dynamically resolved element mappings
//     */
//    private final Map<String, PivotableFieldMapping> dynamicElementMappings = new ConcurrentHashMap<>();
//
//    /**
//     * Statically defined mappings for core schema fields
//     */
//    private final Map<String, PivotableFieldMapping> staticMappings = new ConcurrentHashMap<>();
//
//    // Map of stable aliases for each mapping id
//    private final Map<String, String> aliases = new ConcurrentHashMap<>();
//
//
//    @PostConstruct
//    private void initialize() {
//        // Example: submission.created_date
//        String id = "submission.date";
//        String a = AliasUtil.alias(id);
//        aliases.put(id, a);
//        staticMappings.put(id, new PivotableFieldMapping(
//            id,
//            DATA_SUBMISSION.CREATED_DATE,
//            PivotDataType.DATETIME, Scope.SUBMISSION,
//            List.of(
//                new JoinInfo(DATA_SUBMISSION, ELEMENT_DATA_VALUE.SUBMISSION_ID
//                    .eq(DATA_SUBMISSION.ID))),
//            false
//        ));
//
//        // assignment.org_unit -> needs a join chain: element_data_value.assignment_id -> assignment -> org_unit
//        id = "assignment.org_unit";
//        a = AliasUtil.alias(id);
//        aliases.put(id, a);
//        staticMappings.put(id, new PivotableFieldMapping(
//            id,
//            ORG_UNIT.NAME, // final field
//            PivotDataType.TEXT, Scope.SUBMISSION,
//            List.of(
//                new JoinInfo(ASSIGNMENT,
//                    ELEMENT_DATA_VALUE.ASSIGNMENT_ID.eq(ASSIGNMENT.ID)),
//                new JoinInfo(ORG_UNIT,
//                    ASSIGNMENT.ORG_UNIT_ID.eq(ORG_UNIT.ID))
//            ),
//            false
//        ));
//
//        // assignment.team -> needs a join chain: element_data_value.assignment_id -> assignment -> team
//        id = "assignment.team";
//        a = AliasUtil.alias(id);
//        aliases.put(id, a);
//        staticMappings.put(id, new PivotableFieldMapping(
//            id,
//            TEAM.NAME,
//            PivotDataType.TEXT, Scope.SUBMISSION,
//            List.of(
//                new JoinInfo(ASSIGNMENT,
//                    ELEMENT_DATA_VALUE.ASSIGNMENT_ID.eq(ASSIGNMENT.ID)),
//                new JoinInfo(TEAM,
//                    ASSIGNMENT.TEAM_ID.eq(TEAM.ID))
//            ),
//            false
//        ));
//
//        // assignment.activity -> needs a join chain: element_data_value.assignment_id -> assignment -> activity
//        id = "submission.activity";
//        a = AliasUtil.alias(id);
//        aliases.put(id, a);
//        staticMappings.put(id, new PivotableFieldMapping(
//            id,
//            ACTIVITY.NAME,
//            PivotDataType.TEXT, Scope.SUBMISSION,
//            List.of(
//                new JoinInfo(ASSIGNMENT, ELEMENT_DATA_VALUE.ASSIGNMENT_ID.eq(ASSIGNMENT.ID)),
//                new JoinInfo(ACTIVITY, ASSIGNMENT.ACTIVITY_ID.eq(ACTIVITY.ID))
//            ),
//            false
//        ));
//
//        // submission.count (measure)
//        id = "submission.count";
//        a = AliasUtil.alias(id);
//        aliases.put(id, a);
//        staticMappings.put(id, new PivotableFieldMapping(
//            id,
//            ELEMENT_DATA_VALUE.SUBMISSION_ID, // aggregated
//            PivotDataType.NUMERIC, Scope.SUBMISSION,
//            Collections.emptyList(),
//            true
//        ));
//
//        // add more static mappings as you need...
//    }
//
//    /**
//     * Finds the mapping for a given ID. Handles both static and dynamic element fields.
//     */
//    public Optional<PivotableFieldMapping> findMapping(String id) {
//        if (staticMappings.containsKey(id)) {
//            return Optional.of(staticMappings.get(id));
//        }
//        if (id.startsWith("element.")) {
//            return Optional.of(dynamicElementMappings.computeIfAbsent(id, this::createMappingForElement));
//        }
//        if (id.startsWith("category.")) {
//            // Categories are dynamic but handled separately due to the self-join.
//            return Optional.of(dynamicElementMappings.computeIfAbsent(id, this::createMappingForCategory));
//        }
//        return Optional.empty();
//    }
//
//    /**
//     * Creates a mapping for a standard data element.
//     * Determines scope by checking if the element's configuration is within a repeatable section.
//     */
//    private PivotableFieldMapping createMappingForElement(String fullElementId) {
//        String elementId = fullElementId.replace("element.", "");
//        DataElement element = dataElementRepository.findById(elementId)
//            .orElseThrow(() -> new IllegalArgumentException("Invalid element ID: " + elementId));
//
/// /        For simplicity in this step, we assume Scope.SUBMISSION.
/// /        boolean isRepeatable = formConfRepository.isElementInRepeatableSection(elementId);
//        Scope scope = /*isRepeatable ? Scope.REPEAT_INSTANCE :*/ Scope.SUBMISSION;
//        PivotDataType dataType = mapValueTypeToPivotType(element.getType());
//        Field<?> valueField = getFieldForDataType(dataType, ELEMENT_DATA_VALUE);
//
//        return new PivotableFieldMapping(fullElementId, valueField, dataType, scope, Collections.emptyList(),
//            dataType == PivotDataType.NUMERIC);
//    }
//
//    /**
//     * Creates a mapping for a category dimension, which requires a self-join.
//     */
//    private PivotableFieldMapping createMappingForCategory(String categoryId) {
//        String categoryElementId = categoryId.replace("category.", "");
//        DataElement element = dataElementRepository.findById(categoryElementId)
//            .orElseThrow(() -> new IllegalArgumentException("Invalid category element ID: " +
//                categoryElementId));
//
//        // Create an alias for the element_data_value table to perform the self-join.
//        var categoryValueTable = ELEMENT_DATA_VALUE
//            .as("category_values_" + categoryElementId.hashCode());
//
//        PivotDataType dataType = mapValueTypeToPivotType(element.getType());
//        Field<?> valueField = getFieldForDataType(dataType, categoryValueTable);
//
//        // The join links the main row to its corresponding
//        // category value row within the same repeat instance.
//        JoinInfo selfJoin = new JoinInfo(categoryValueTable,
//            ELEMENT_DATA_VALUE.SUBMISSION_ID
//                .eq(categoryValueTable.field(ELEMENT_DATA_VALUE.SUBMISSION_ID))
//
//                .and(ELEMENT_DATA_VALUE.REPEAT_INSTANCE_ID
//                    .eq(categoryValueTable.field(ELEMENT_DATA_VALUE.REPEAT_INSTANCE_ID)))
//
//                .and(ELEMENT_DATA_VALUE.CATEGORY_ID
//                    .eq(categoryValueTable.field(ELEMENT_DATA_VALUE.ELEMENT_ID)))
//
//                .and(categoryValueTable.field(ELEMENT_DATA_VALUE.ELEMENT_ID)
//                    .eq(categoryElementId))
//
//        );
//
//        // A category is always at the repeat instance scope.
//        return new PivotableFieldMapping(categoryId, valueField, dataType, Scope.REPEAT_INSTANCE, List.of(selfJoin), false);
//    }
//
//    private Field<?> getFieldForDataType(PivotDataType dataType, ElementDataValue elementDataValue) {
//        return switch (dataType) {
//            case NUMERIC -> elementDataValue.VALUE_NUM;
//            case BOOLEAN -> elementDataValue.VALUE_BOOL;
//            // Add cases for DATE, DATETIME when those columns exist
//            default -> elementDataValue.VALUE_TEXT;
//        };
//    }
//
//    /**
//     * map specific domain enum to pivot enum
//     *
//     * @param valueType domain value type
//     * @return pivot value type
//     */
//    private PivotDataType mapValueTypeToPivotType(ValueType valueType) {
//        return switch (valueType) {
//            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
//                 IntegerNegative, IntegerZeroOrPositive -> PivotDataType.NUMERIC;
//            case Boolean, TrueOnly -> PivotDataType.BOOLEAN;
//            case Date -> PivotDataType.DATE;
//            case DateTime -> PivotDataType.DATETIME;
//            default -> PivotDataType.TEXT;
//        };
//    }
//}
