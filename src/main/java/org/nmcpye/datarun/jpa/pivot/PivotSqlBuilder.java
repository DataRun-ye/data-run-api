////package org.nmcpye.datarun.jpa.pivot;
////
/////**
//// * @author Hamza Assada - 7amza.it@gmail.com
//// * @since 24/08/2025
//// */
////
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.jooq.Record;
////import org.jooq.*;
////import org.nmcpye.datarun.jpa.pivot.dto.PivotQueryRequest;
////import org.springframework.stereotype.Component;
////
////import java.math.BigDecimal;
////import java.util.ArrayList;
////import java.util.List;
////
////import static org.jooq.impl.DSL.*;
////import static org.nmcpye.datarun.jooq.Tables.ELEMENT_DATA_VALUE;
////import static org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS;
////import static org.nmcpye.datarun.jpa.pivot.dto.PivotParameters.*;
////
/////**
//// * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
//// */
////@Slf4j
////@Component
////@RequiredArgsConstructor
////public class PivotSqlBuilder {
////
////    private final DSLContext dsl;
////    private final PivotRegistry registry;
////
////    public Select<?> buildQuery(PivotQueryRequest request) {
////        SelectQuery<Record> query = dsl.selectQuery();
////        query.addFrom(PIVOT_GRID_FACTS);
////
////        // Resolve mappings for all dimensions and measures
////        PivotableFieldMapping rowDimMapping = registry.findMapping(request.rows().get(0).dimensionId())
////            .orElseThrow(() -> new IllegalArgumentException("Invalid row dimension"));
////
////        PivotableFieldMapping colDimMapping = request.columns().isEmpty() ? null :
////            registry.findMapping(request.columns().get(0).dimensionId())
////                .orElseThrow(() -> new IllegalArgumentException("Invalid column dimension"));
////
////        // --- Build SELECT Clause ---
////        query.addSelect(rowDimMapping.dimensionField().as("row_dim"));
////        if (colDimMapping != null) {
////            query.addSelect(colDimMapping.dimensionField().as("col_dim"));
////        }
////
////        // --- Build Aggregations with FILTER clause ---
////        for (MeasureRef measure : request.measures()) {
////            PivotableFieldMapping measureMapping = registry.findMapping(measure.id())
////                .orElseThrow(() -> new IllegalArgumentException("Invalid measure: " + measure.id()));
////
////            query.addSelect(buildAggregation(measure, measureMapping)
////                .as(measure.id() + "_" + measure.aggregation()));
////        }
////
////        // --- Build WHERE, GROUP BY, ORDER BY, and LIMIT clauses ---
////        query.addConditions(buildFilterCondition(request.filters()));
////
////        query.addGroupBy(rowDimMapping.dimensionField());
////        if (colDimMapping != null) {
////            query.addGroupBy(colDimMapping.dimensionField());
////        }
////
////        // Apply sorting and pagination...
////
////        return query;
////    }
////
////    private Field<?> buildAggregation(MeasureRef measure, PivotableFieldMapping mapping) {
////        SelectField<?> fieldToAggregate;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.jooq.DSLContext;
//import org.jooq.Field;
//import org.springframework.stereotype.Component;
//
//import java.util.Objects;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//////        List<SelectField<?>> selectFields = new ArrayList<>();
////        if (measure.id().equals("submission.count")) {
////            // Special case for counting distinct submissions
////            fieldToAggregate = countDistinct(mapping.measureField());
////        } else {
////            // Get the corresponding field from the CTE
////            Field<BigDecimal> valueField = (Field<BigDecimal>) cte.field(mapping.field().getName(), BigDecimal.class);
////            measureField = switch (measure.aggregation()) {
////                case SUM -> sum(valueField);
////                case AVG -> avg(valueField);
////                case COUNT -> count(valueField);
////                case MIN -> min(valueField);
////                case MAX -> max(valueField);
////            };
////            // General case for SUM, AVG, etc.
////            Field<BigDecimal> measureField = (Field<BigDecimal>) mapping.measureField();
////            fieldToAggregate = switch (measure.aggregation()) {
////                case SUM -> sum(measureField);
////                case AVG -> avg(measureField);
////                case COUNT -> count(measureField);
////                case MIN -> min(measureField);
////                case MAX -> max(measureField);
////            };
////        }
////
////        // This is the key: Apply the FILTER condition. For static measures, it will be noCondition().
////        return fieldToAggregate.filter(mapping.condition());
////    }
////
////    private Condition buildFilterCondition(List<FilterRef> filters) {
////        Condition condition = trueCondition();
////        for (FilterRef filter : filters) {
////            PivotableFieldMapping mapping = registry.findMapping(filter.id())
////                .orElseThrow(() -> new IllegalArgumentException("Invalid filter ID: " + filter.id()));
////
////            condition = condition.and(
////                // Logic to build jOOQ condition from filter operator and values...
////                mapping.dimensionField().in(filter.values())
////            );
////        }
////        return condition;
////    }
////
////
////    ///
////    /**
////     * (Phase 2) The Outer Query - "Aggregating": Read from the flattened CTE and perform the final
////     * aggregations <code>SUM, COUNT, etc.</code>, grouping, sorting, and pagination.
////     */
////    private Select<?> createOuterQuery(QueryContext ctx, CommonTableExpression<?> cte) {
////        List<SelectField<?>> selectFields = new ArrayList<>();
////
////        // Add the row and column dimensions to the SELECT and GROUP BY clauses
////        selectFields.add(cte.field(ctx.rowDimensionField));
////
////        List<GroupField> groupByFields = new ArrayList<>();
////        groupByFields.add(cte.field(ctx.rowDimensionField));
////
////        if (ctx.columnDimensionField != null) {
////            selectFields.add(cte.field(ctx.columnDimensionField));
////            groupByFields.add(cte.field(ctx.columnDimensionField));
////        }
////
////        // Add the final aggregation functions for each measure
////        for (int i = 0; i < ctx.request().measures().size(); i++) {
////            MeasureRef measure = ctx.request().measures().get(i);
////            PivotableFieldMapping mapping = ctx.allMappings.get(measure.id());
////
////            Field<?> measureField;
////            if (measure.id().equals("submission.count")) {
////                // Special handling for counting distinct submissions
////                measureField = countDistinct(cte.field(ELEMENT_DATA_VALUE.SUBMISSION_ID));
////            } else {
////                // Get the corresponding field from the CTE
////                Field<BigDecimal> valueField = (Field<BigDecimal>) cte.field(mapping.field().getName(), BigDecimal.class);
////                measureField = switch (measure.aggregation()) {
////                    case SUM -> sum(valueField);
////                    case AVG -> avg(valueField);
////                    case COUNT -> count(valueField);
////                    case MIN -> min(valueField);
////                    case MAX -> max(valueField);
////                };
////            }
////            selectFields.add(measureField.as(measure.id() + "_" + measure.aggregation()));
////        }
////
////        SelectQuery<Record> outerQuery = dsl.selectQuery();
////        outerQuery.addFrom(cte);
////        outerQuery.addSelect(selectFields);
////        outerQuery.addGroupBy(groupByFields);
////
////        // Apply final sorting and pagination
////        applySortingAndPagination(outerQuery, ctx, cte);
////
////        return outerQuery;
////    }
////
////    private void applySortingAndPagination(SelectQuery<?> query, QueryContext ctx, CommonTableExpression<?> cte) {
////        // Sorting logic (can be expanded for measures)
////        if (ctx.request().sorting() != null &&
////            ctx.request().sorting().id().equals(ctx.request().rows().get(0).dimensionId())) {
////            Field<?> sortField = cte.field(ctx.rowDimensionField);
////
////            if (sortField != null) {
////                SortField<?> jooqSortField = ctx.request().sorting().direction() == SortDirection.ASC
////                    ? sortField.asc()
////                    : sortField.desc();
////                query.addOrderBy(jooqSortField);
////            }
////        }
////
////        // Pagination
////        if (ctx.request().pagination() != null) {
////            query.addLimit(ctx.request().pagination().pageSize());
////            query.addOffset(ctx.request().pagination().page() * ctx.request().pagination().pageSize());
////        }
////    }
////}
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PivotSqlBuilder {
//    private final DSLContext dsl;
//    private final PivotRegistry registry;
//
//    /**
//     * A Helper to Pass State: Instead of passing many parameters between
//     * methods, we use this simple context object.
//     */
//    private record QueryContext(
//        PivotQueryRequest request,
//        Map<String, PivotableFieldMapping> allMappings,
//        Field<String> rowDimensionField,
//        @Nullable Field<String> columnDimensionField,
//        List<Field<?>> measureValueFields,
//        List<PivotableFieldMapping> directDimensionMappings) {
//    }
//
//    /**
//     * Main entry point. Constructs the complete jOOQ query.
//     */
//    public Select<?> buildQuery(PivotQueryRequest request) {
//        // 1. Resolve all identifiers from the request against the registry.
//        QueryContext queryContext = prepareQueryContext(request);
//
//        // 2. Build the inner "flattening" query (CTE).
//        CommonTableExpression<?> baseCte = createInnerQueryCte(queryContext);
//
//        // 3. Build the outer "aggregating" query that selects from the CTE.
//        return createOuterQuery(queryContext, baseCte);
//    }
//
//    private QueryContext prepareQueryContext(PivotQueryRequest request) {
//        // Collect all unique IDs from rows, cols, measures, filters
//        Set<String> allIds = Stream.of(
//                request.rows().stream().map(DimensionRef::dimensionId),
//                request.columns().stream().map(DimensionRef::dimensionId),
//                request.measures().stream().map(MeasureRef::id),
//                request.filters().stream().map(FilterRef::id))
//            .flatMap(s -> s)
//            .collect(Collectors.toSet());
//
//        // Resolve all mappings at once. Fail fast if any ID is invalid.
//        Map<String, PivotableFieldMapping> allMappings = allIds.stream()
//            .collect(Collectors.toMap(
//                id -> id,
//                id -> registry.findMapping(id)
//                    .orElseThrow(() -> new IllegalArgumentException("Unknown pivot ID: " + id))
//            ));
//
//        // For simplicity in this step, we assume one row and max one column dimension.
//        String rowDimId = request.rows().get(0).dimensionId();
//        String colDimId = request.columns().isEmpty() ? null : request.columns().get(0).dimensionId();
//
//        //noinspection unchecked
//        Field<String> rowDimField = (Field<String>) allMappings.get(rowDimId).field().as("row_dim");
//        //noinspection unchecked
//        Field<String> colDimField = (colDimId == null) ? null :
//            (Field<String>) allMappings.get(colDimId).field().as("col_dim");
//
//        // Identify fields that come directly from joined tables vs. pivoted from elements
//        List<PivotableFieldMapping> directDimensionMappings = Stream.of(rowDimId, colDimId)
//            .filter(Objects::nonNull)
//            .map(allMappings::get)
//            .filter(m -> !m.id().startsWith("element."))
//            .toList();
//
//        // Placeholder fields for measures within the CTE
//        List<Field<?>> measureValueFields = request.measures().stream()
//            .<Field<?>>map(m -> allMappings.get(m.id()).field().as("measure_" + m.id().hashCode()))
//            .toList();
//
//        return new QueryContext(request, allMappings, rowDimField, colDimField, measureValueFields, directDimensionMappings);
//    }
//
//    /**
//     * Phase 1. The Inner Query (CTE) - "Flattening": Transform the vertical data from <code>element_data_value</code>
//     * into a horizontal, tabular result set. Each row in this CTE will represent a single <code>data_submission</code>.
//     * The CTE will have a column for <code>submission_id</code>, a column for each requested dimension (e.g., <code>org_unit_name</code>),
//     * and a column for each requested measure's base value (e.g., the numeric value of `element.Jck63XyG19y`).
//     * This is achieved using conditional aggregation (<code>MAX(CASE WHEN ... THEN ... END)</code>) grouped by the
//     * submission ID and any direct dimension fields.
//     *
//     * @param ctx context
//     * @return The Inner Query (CTE) expression (Phase 1).
//     */
//    private CommonTableExpression<?> createInnerQueryCte(QueryContext ctx) {
//        Name cteName = name("flattened_data");
//
//        // Start building the SELECT list for the CTE
//        List<SelectField<?>> selectFields = new ArrayList<>();
//        selectFields.add(ELEMENT_DATA_VALUE.SUBMISSION_ID);
//
//        // Add direct dimension fields (e.g., ORG_UNIT.NAME)
//        ctx.directDimensionMappings.forEach(mapping -> selectFields.add(mapping.field()));
//
//        // Add pivoted dimension/measure fields using conditional aggregation
//        ctx.allMappings.values().stream()
//            .filter(m -> m.id().startsWith("element."))
//            .forEach(mapping -> {
//                Condition condition = ELEMENT_DATA_VALUE.ELEMENT_ID.eq(mapping.id().replace("element.", ""));
//                // We use MAX() as the aggregation function here simply to pick one value.
//                // The actual aggregation (SUM, etc.) happens in the outer query.
//                selectFields.add(max(when(condition, mapping.field())).as(mapping.field().getName()));
//            });
//
//        // Build the query
//        SelectQuery<Record> innerQuery = dsl.selectQuery();
//        innerQuery.addSelect(selectFields);
//        innerQuery.addFrom(ELEMENT_DATA_VALUE);
//
//        // Add necessary joins and filters
//        applyJoins(innerQuery, ctx.allMappings.values());
//        applyFilters(innerQuery, ctx);
//
//        // Group by submission and any direct dimensions
//        innerQuery.addGroupBy(ELEMENT_DATA_VALUE.SUBMISSION_ID);
//        ctx.directDimensionMappings.forEach(mapping -> innerQuery.addGroupBy(mapping.field()));
//
//        return cteName.as(innerQuery);
//    }
//
//    private void applyJoins(SelectQuery<?> query, Collection<PivotableFieldMapping> mappings) {
//        // Use a map to ensure we only join each table once
//        Map<Table<?>, JoinInfo> requiredJoins = new HashMap<>();
//        mappings.stream()
//            .flatMap(mapping -> mapping.requiredJoins().stream())
//            .forEach(joinInfo -> requiredJoins.putIfAbsent(joinInfo.table(), joinInfo));
//
//        requiredJoins.values().forEach(joinInfo ->
//            query.addJoin(
//                joinInfo.table(),
//                JoinType.LEFT_OUTER_JOIN, // Using INNER JOIN to only include data that matches dimensions
//                joinInfo.onCondition()
//            )
//        );
//    }
//
//    private void applyFilters(SelectQuery<?> query, QueryContext ctx) {
//        Condition condition = trueCondition();
//        for (FilterRef filter : ctx.request().filters()) {
//            PivotableFieldMapping mapping = ctx.allMappings.get(filter.id());
//            Field field = mapping.field();
//
//            // Build condition based on operator
//            condition = condition.and(switch (filter.operator()) {
//                case EQUALS -> field.eq(filter.values().get(0));
//                case IN -> field.in(filter.values());
//                case BETWEEN -> field.between(filter.values().get(0), filter.values().get(1));
//                // Add other operators (GT, LT, etc.) as needed
//                default ->
//                    throw new UnsupportedOperationException("Filter operator not supported: " + filter.operator());
//            });
//        }
//        query.addConditions(condition);
//    }
//
//    /**
//     * (Phase 2) The Outer Query - "Aggregating": Read from the flattened CTE and perform the final
//     * aggregations <code>SUM, COUNT, etc.</code>, grouping, sorting, and pagination.
//     */
//    private Select<?> createOuterQuery(QueryContext ctx, CommonTableExpression<?> cte) {
//        List<SelectField<?>> selectFields = new ArrayList<>();
//
//        // Add the row and column dimensions to the SELECT and GROUP BY clauses
//        selectFields.add(cte.field(ctx.rowDimensionField));
//
//        List<GroupField> groupByFields = new ArrayList<>();
//        groupByFields.add(cte.field(ctx.rowDimensionField));
//
//        if (ctx.columnDimensionField != null) {
//            selectFields.add(cte.field(ctx.columnDimensionField));
//            groupByFields.add(cte.field(ctx.columnDimensionField));
//        }
//
//        // Add the final aggregation functions for each measure
//        for (int i = 0; i < ctx.request().measures().size(); i++) {
//            MeasureRef measure = ctx.request().measures().get(i);
//            PivotableFieldMapping mapping = ctx.allMappings.get(measure.id());
//
//            Field<?> measureField;
//            if (measure.id().equals("submission.count")) {
//                // Special handling for counting distinct submissions
//                measureField = countDistinct(cte.field(ELEMENT_DATA_VALUE.SUBMISSION_ID));
//            } else {
//                // Get the corresponding field from the CTE
//                Field<BigDecimal> valueField = (Field<BigDecimal>) cte.field(mapping.field().getName(), BigDecimal.class);
//                measureField = switch (measure.aggregation()) {
//                    case SUM -> sum(valueField);
//                    case AVG -> avg(valueField);
//                    case COUNT -> count(valueField);
//                    case MIN -> min(valueField);
//                    case MAX -> max(valueField);
//                };
//            }
//            selectFields.add(measureField.as(measure.id() + "_" + measure.aggregation()));
//        }
//
//        SelectQuery<Record> outerQuery = dsl.selectQuery();
//        outerQuery.addFrom(cte);
//        outerQuery.addSelect(selectFields);
//        outerQuery.addGroupBy(groupByFields);
//
//        // Apply final sorting and pagination
//        applySortingAndPagination(outerQuery, ctx, cte);
//
//        return outerQuery;
//    }
//
//    private void applySortingAndPagination(SelectQuery<?> query, QueryContext ctx, CommonTableExpression<?> cte) {
//        // Sorting logic (can be expanded for measures)
//        if (ctx.request().sorting() != null &&
//            ctx.request().sorting().id().equals(ctx.request().rows().get(0).dimensionId())) {
//            Field<?> sortField = cte.field(ctx.rowDimensionField);
//
//            if (sortField != null) {
//                SortField<?> jooqSortField = ctx.request().sorting().direction() == SortDirection.ASC
//                    ? sortField.asc()
//                    : sortField.desc();
//                query.addOrderBy(jooqSortField);
//            }
//        }
//
//        // Pagination
//        if (ctx.request().pagination() != null) {
//            query.addLimit(ctx.request().pagination().pageSize());
//            query.addOffset(ctx.request().pagination().page() * ctx.request().pagination().pageSize());
//        }
//    }
//}
