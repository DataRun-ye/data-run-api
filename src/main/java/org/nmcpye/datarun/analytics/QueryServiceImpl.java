package org.nmcpye.datarun.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Result;
import org.jooq.Select;
import org.nmcpye.datarun.analytics.dto.*;
import org.nmcpye.datarun.analytics.exception.AnalyticsQueryException;
import org.nmcpye.datarun.analytics.fieldresolver.MappedQueryableElement;
import org.nmcpye.datarun.analytics.model.ValidatedMeasure;
import org.nmcpye.datarun.analytics.util.AliasSanitizer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryServiceImpl implements QueryService {

    private final MetadataService metadataService;
    private final MeasureValidationService measureValidationService;
    private final JooQQueryBuilder jooQQueryBuilder;
    private final DSLContext dsl;
    private final QueryJooqMapper fieldMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResponse query(QueryRequest request,
                               GridResponseFormat format,
                               Set<String> allowedTeamUids) {
        try {
            // 1) Validate request basic sanity
            Objects.requireNonNull(request, "PivotQueryRequest is required");
            Objects.requireNonNull(request.getTemplateId(), "templateId is required");
            Objects.requireNonNull(request.getTemplateVersionId(), "templateVersionId is required");
            if (format == null) format = GridResponseFormat.TABLE_ROWS;

            // STEP 1: Fetch metadata ONCE and create a lookup map for efficient O(1) access.
            // This is a huge improvement over the previous implementation.
            Map<String, QueryableElement> fieldMap = metadataService
                .getMetadataForTemplate(request.getTemplateId(), request.getTemplateVersionId())
                .getAvailableFields().stream()
                .collect(Collectors.toMap(QueryableElement::id, f -> f));

            // STEP 2: Validate measures. This service is now simpler and relies on the new metadata contract.
            List<ValidatedMeasure> validatedMeasures = AliasSanitizer
                .ensureUniqueAliasesWithRename(
                    Optional.ofNullable(request.getMeasures())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(mr -> measureValidationService
                            .validate(mr, request.getTemplateId(),
                                request.getTemplateVersionId()))
                        .collect(Collectors.toList()),
                    Boolean.TRUE.equals(request.getAutoRenameAliases())
                );

            // 3) Determine grouping dimensions
            List<String> rowDims = Optional.ofNullable(request.getRowDimensions()).orElse(Collections.emptyList());
            List<String> colDims = Optional.ofNullable(request.getColumnDimensions()).orElse(Collections.emptyList());
            List<String> legacyDims = Optional.ofNullable(request.getDimensions()).orElse(Collections.emptyList());

            if (rowDims.isEmpty() && colDims.isEmpty() && !legacyDims.isEmpty()) {
                // Backwards compat: treat legacy dims as row dims
                rowDims = legacyDims;
            }

            // combined group-by dims (row + column)
            List<String> groupByDims = Stream.concat(rowDims.stream(), colDims.stream())
                .collect(Collectors.toList());

            long totalGroups = -1;
            try {
                totalGroups = jooQQueryBuilder.countGroups(
                    groupByDims,
                    validatedMeasures,
                    convertFilters(request.getFilters()),
                    request.getFrom(),
                    request.getTo(),
                    allowedTeamUids
                );
            } catch (Exception ex) {
                log.warn("Failed to compute totalGroups: {}", ex.getMessage());
                totalGroups = -1;
            }

            // 4) Build the jOOQ Select using PivotQueryBuilder
            Select<org.jooq.Record> select = jooQQueryBuilder.buildSelect(
                groupByDims,
                validatedMeasures,
                convertFilters(request.getFilters()),
                request.getFrom(),
                request.getTo(),
                convertSorts(request.getSorts()),
                request.getLimit(),
                request.getOffset(),
                allowedTeamUids
            );

            log.debug("Executing pivot select: \n{}", dsl.renderInlined(select));

            // 5) Execute
            Result<org.jooq.Record> result = dsl.fetch(select);

            // 6) Convert Result -> requested format
            QueryResponse.QueryResponseBuilder respB = QueryResponse.builder()
                .meta(Map.of(
                    "format", format.name(),
                    "templateId", request.getTemplateId(),
                    "templateVersionId", request.getTemplateVersionId()))
                .total(totalGroups);

            if (format == GridResponseFormat.TABLE_ROWS) {
                // Columns: groupBy dims then measures (aliases)
                // STEP 3: Build response columns using the efficient lookup map.
                List<ColumnDto> columns = buildColumnsForTable(groupByDims, validatedMeasures, fieldMap);
                List<Map<String, Object>> rows = mapResultToRows(result, columns);
                respB.columns(columns).rows(rows);
                // total optional: not calculated now (could be separate count query)
            } else {
                // PIVOT_MATRIX
                PivotMatrixDto matrix = buildMatrixFromResult(result, rowDims, colDims, validatedMeasures);
                respB.matrix(matrix);
            }

            return respB.build();
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid pivot request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Pivot query failed", ex);
            throw new AnalyticsQueryException("Pivot execution failed: " + ex.getMessage(), ex);
        }
    }

    /* --------------------- helpers --------------------- */

    private List<FilterDto> convertFilters(List<FilterDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        return dtos.stream().map(fdto ->
                new FilterDto(fdto.field(), fdto.op(), fdto.value()))
            .collect(Collectors.toList());
    }

    private List<SortDto> convertSorts(List<SortDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        return dtos.stream().map(sdto ->
                new SortDto(sdto.fieldOrAlias(), Boolean.TRUE.equals(sdto.desc())))
            .collect(Collectors.toList());
    }


    /**
     * Build columns list for TABLE_ROWS response.
     * It uses the pre-fetched fieldMap for instant lookups instead of re-scanning metadata.
     */
//    private List<ColumnDto> buildColumnsForTable(List<String> groupByDims,
//                                                 List<ValidatedMeasure> measures,
//                                                 Map<String, QueryableElement> fieldMap) {
//        List<ColumnDto> cols = new ArrayList<>();
//
//        // Add columns for dimensions
//        for (String dimId : groupByDims) {
//            QueryableElement dto = fieldMap.get(dimId);
//            String name = (dto != null) ? dto.name() : dimId; // Fallback to ID
//            DataType dataType = (dto != null) ? dto.dataType() : DataType.TEXT; // Fallback
//            cols.add(ColumnDto.builder()
//                .id(dimId)
//                .label(name)
//                .dataType(dataType)
//                .build());
//        }
//
//        // Add columns for measures
//        for (ValidatedMeasure vm : measures) {
//            // Reconstruct the measure's ID to look it up in the map for a proper label.
//            String measureId = vm.effectiveMode().equals("TEMPLATE") ? "etc:" + vm.etcUid() : "de:" + vm.deUid();
//            QueryableElement dto = fieldMap.get(measureId);
//
//            String label = vm.alias();
//            if (dto != null && vm.alias()
//                .startsWith(AnalyticsField.from(dto.id()).value())) {
//                // If using a default alias, construct a nicer label e.g., "Age of Head (Sum)"
//                label = String.format("%s (%s)", dto.label(), vm.aggregation().name());
//            }
//
//            cols.add(ColumnDto.builder()
//                .id(vm.alias())
//                .label(label)
//                .dataType(determineMeasureDataType(vm))
//                .build());
//        }
//
//        return cols;
//    }
    private List<ColumnDto> buildColumnsForTable(List<String> groupByDims,
                                                 List<ValidatedMeasure> measures,
                                                 Map<String, QueryableElement> fieldMap) {
        List<ColumnDto> cols = new ArrayList<>();

        // Add columns for dimensions
        for (String dimId : groupByDims) {
            QueryableElement dto = fieldMap.get(dimId);
            String name = (dto != null) ? dto.name() : dimId; // Fallback to ID for label
            var label = (dto != null) ? dto.label() : null; // Fallback to ID for label
            DataType dataType = (dto != null) ? dto.dataType() : DataType.TEXT; // Fallback

            // compute SQL-safe alias for this dimension so it matches jOOQ Record keys
//            String sqlAlias = AliasSanitizer.sanitize(dimId);
            MappedQueryableElement field = MappedQueryableElement.from(dimId);

            cols.add(ColumnDto.builder()
                .id(dimId)            // logical id (stable for clients)
                .key(field.value())        // SQL alias used to read from Record
                .name(name)
                .label(label)          // human friendly label
                .dataType(dataType)
                .build());
        }

        // Add columns for measures
        for (ValidatedMeasure vm : measures) {
            String rawAlias = vm.alias(); // logical alias, used by clients
            String sqlAlias = AliasSanitizer.sanitize(rawAlias); // sql-safe key

            QueryableElement dto = null;
            String measureFieldLookup = "etc:" + vm.etcUid();
            dto = fieldMap.get(measureFieldLookup);

            String name = rawAlias;
            Map<String, String> label = null;
            if (dto != null) {
                label = dto.label();
                if (rawAlias.startsWith(MappedQueryableElement.from(dto.id()).value())) {
                    name = String.format("%s (%s)", dto.name(), vm.aggregation().name());
                } else {
                    name = dto.name();
                }
            }

            cols.add(ColumnDto.builder()
                .id(rawAlias)            // keep logical alias as id for client stability
                .key(sqlAlias)           // sanitized SQL alias
                .name(name)
                .label(label)
                .dataType(determineMeasureDataType(vm))
                .build());
        }

        return cols;
    }

    private DataType determineMeasureDataType(ValidatedMeasure vm) {
        // pick reasonable default data type name used by fieldMapper
        Field<?> tf = vm.targetField();
        if (tf == null) return DataType.TEXT;
        Class<?> javaType = tf.getDataType() != null ? tf.getDataType().getType() : tf.getType();
        if (Number.class.isAssignableFrom(javaType) || BigDecimal.class.isAssignableFrom(javaType))
            return DataType.NUMERIC;
        if (Boolean.class.isAssignableFrom(javaType)) return DataType.BOOLEAN;
        if (java.time.LocalDateTime.class.isAssignableFrom(javaType)) return DataType.TIMESTAMP;
        return DataType.TEXT;
    }

    /**
     * Map jOOQ Result<org.jooq.Record> to table rows. Each ColumnDto.id is used to pull value
     * from the org.jooq.Record (alias or column name).
     */
    private List<Map<String, Object>> mapResultToRows(Result<org.jooq.Record> result,
                                                      List<ColumnDto> columns) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (org.jooq.Record r : result) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (ColumnDto c : columns) {
                Object val = null;
                String lookupKey = c.getKey(); // preferred SQL alias
                String logicalId = c.getId();

                // Attempt lookups in order: key -> id -> sanitized(key) -> fallback value retrieval
                if (lookupKey != null) {
                    try {
                        val = r.get(lookupKey);
                    } catch (Exception ignored) {
                        val = null;
                    }
                }

                if (val == null && logicalId != null) {
                    try {
                        val = r.get(logicalId);
                    } catch (Exception ignored) {
                        val = null;
                    }
                }

                if (val == null && lookupKey != null) {
                    try {
                        // as a final attempt, try sanitized version of the logical id (if different)
                        String sanitizedFromId = AliasSanitizer.sanitize(logicalId);
                        if (!sanitizedFromId.equals(lookupKey)) {
                            val = r.get(sanitizedFromId);
                        }
                    } catch (Exception ignored) {
                        val = null;
                    }
                }

                if (val == null) {
                    try {
                        // last resort - getValue keyed by column name; may throw if name invalid
                        val = r.getValue(lookupKey != null ? lookupKey : logicalId);
                    } catch (Exception ignored) {
                        val = null;
                    }
                }

                // Put the value under the client-facing logical id so client sees columns[].id keys.
                row.put(logicalId, val);
            }
            rows.add(row);
        }
        return rows;
    }
//    private List<Map<String, Object>> mapResultToRows(Result<org.jooq.Record> result,
//                                                      List<ColumnDto> columns) {
//        List<Map<String, Object>> rows = new ArrayList<>();
//        for (org.jooq.Record r : result) {
//            Map<String, Object> row = new LinkedHashMap<>();
//            for (ColumnDto c : columns) {
//                Object val;
//                try {
//                    // Prefer typed get by alias/name
//                    val = r.get(c.getId());
//                } catch (Exception e) {
//                    // fallback to safe retrieval
//                    try {
//                        val = r.getValue(c.getId());
//                    } catch (Exception ex) {
//                        val = null;
//                    }
//                }
//                row.put(c.getId(), val);
//            }
//            rows.add(row);
//        }
//        return rows;
//    }

    /**
     * Convert grouped result set into a pivot matrix.
     * Algorithm:
     * - compute ordered row keys and column keys (String key from dimension values)
     * - build mapping rowKey -> colKey -> map(alias->value)
     * - materialize rowHeaders/colHeaders and cell matrix
     */
    private PivotMatrixDto buildMatrixFromResult(Result<org.jooq.Record> result,
                                                 List<String> rowDims,
                                                 List<String> colDims,
                                                 List<ValidatedMeasure> measures) {

        // ordered maps keep insertion order
        LinkedHashMap<String, List<String>> rowKeyToValues = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> colKeyToValues = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, Object>>> grid = new HashMap<>();

        for (org.jooq.Record r : result) {
            List<String> rowValues = rowDims.stream().map(MappedQueryableElement::from).map(MappedQueryableElement::value).map(d -> stringify(r.get(d))).collect(Collectors.toList());
            List<String> colValues = colDims.stream().map(MappedQueryableElement::from).map(MappedQueryableElement::value).map(d -> stringify(r.get(d))).collect(Collectors.toList());

            String rowKey = makeCompoundKey(rowValues);
            String colKey = makeCompoundKey(colValues);

            rowKeyToValues.putIfAbsent(rowKey, rowValues);
            colKeyToValues.putIfAbsent(colKey, colValues);

            Map<String, Map<String, Object>> rowMap = grid.computeIfAbsent(rowKey, k -> new HashMap<>());
            Map<String, Object> cell = rowMap.computeIfAbsent(colKey, k -> new HashMap<>());

            for (ValidatedMeasure vm : measures) {
                Object v = r.get(vm.alias());
                cell.put(vm.alias(), v);
            }
        }

        // materialize ordered header lists
        List<List<String>> rowHeaders = new ArrayList<>(rowKeyToValues.values());
        List<List<String>> columnHeaders = new ArrayList<>(colKeyToValues.values());

        // build matrix cells
        List<List<Map<String, Object>>> cells = new ArrayList<>();
        for (String rowKey : rowKeyToValues.keySet()) {
            List<Map<String, Object>> rowCells = new ArrayList<>();
            for (String colKey : colKeyToValues.keySet()) {
                Map<String, Map<String, Object>> rowMap = grid.get(rowKey);
                Map<String, Object> cell = rowMap != null ? rowMap.get(colKey) : null;
                rowCells.add(cell == null ? Collections.emptyMap() : cell);
            }
            cells.add(rowCells);
        }

        return PivotMatrixDto.builder()
            .rowDimensionNames(rowDims)
            .columnDimensionNames(colDims)
            .measureAliases(measures.stream().map(ValidatedMeasure::alias).collect(Collectors.toList()))
            .rowHeaders(rowHeaders)
            .columnHeaders(columnHeaders)
            .cells(cells)
            .build();
    }

    private String makeCompoundKey(List<String> values) {
        // simple safe encoding: JSON-style join using | char escaped
        if (values == null || values.isEmpty()) return "";
        return String.join("||", values.stream().map(v -> v == null ? "" : v).collect(Collectors.toList()));
    }

    private String stringify(Object v) {
        return v == null ? "" : v.toString();
    }
}

