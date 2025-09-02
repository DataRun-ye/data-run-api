package org.nmcpye.datarun.analytics.pivot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Result;
import org.jooq.Select;
import org.nmcpye.datarun.analytics.pivot.dto.*;
import org.nmcpye.datarun.analytics.pivot.model.ValidatedMeasure;
import org.nmcpye.datarun.analytics.pivot.util.AliasSanitizer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * PivotQueryService implementation that:
 * - validates measures in template-context
 * - delegates SQL building to PivotQueryBuilder
 * - transforms jOOQ Result<org.jooq.Record> into TABLE_ROWS or PIVOT_MATRIX formats
 * <p>
 * Assumes: PivotQueryRequest, FilterDto, SortDto, PivotQueryBuilder, MeasureValidationService,
 * PivotMetadataService, PivotFieldJooqMapper, ValidatedMeasure exist per our design.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PivotQueryServiceImpl implements PivotQueryService {

    private final PivotMetadataService metadataService;
    private final MeasureValidationService measureValidationService;
    private final PivotQueryBuilder queryBuilder;
    private final DSLContext dsl;
    private final PivotFieldJooqMapper fieldMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public PivotQueryResponse query(PivotQueryRequest request,
                                    PivotOutputFormat format,
                                    Set<String> allowedTeamUids) {
        try {
            // 1) Validate request basic sanity
            Objects.requireNonNull(request, "PivotQueryRequest is required");
            Objects.requireNonNull(request.getTemplateId(), "templateId is required");
            Objects.requireNonNull(request.getTemplateVersionId(), "templateVersionId is required");
            if (format == null) format = PivotOutputFormat.TABLE_ROWS;

            // 2) Validate and convert measures
            List<ValidatedMeasure> validatedMeasures = AliasSanitizer
                .ensureUniqueAliasesWithRename(Optional.ofNullable(request.getMeasures())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(mr -> measureValidationService.validate(mr, request.getTemplateId(), request.getTemplateVersionId()))
                        .collect(Collectors.toList()),
                    Boolean.TRUE.equals(request.getAutoRenameAliases()));

            // 3) Determine grouping dimensions
            List<String> rowDims = Optional.ofNullable(request.getRowDimensions()).orElse(Collections.emptyList());
            List<String> colDims = Optional.ofNullable(request.getColumnDimensions()).orElse(Collections.emptyList());
            List<String> legacyDims = Optional.ofNullable(request.getDimensions()).orElse(Collections.emptyList());

            if (rowDims.isEmpty() && colDims.isEmpty() && !legacyDims.isEmpty()) {
                // Backwards compat: treat legacy dims as row dims
                rowDims = legacyDims;
            }

            // combined group-by dims (row + column)
            List<String> groupByDims = Stream.concat(rowDims.stream(), colDims.stream()).collect(Collectors.toList());

            long totalGroups = -1;
            try {
                totalGroups = queryBuilder.countGroups(
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
            Select<org.jooq.Record> select = queryBuilder.buildSelect(
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
            PivotQueryResponse.PivotQueryResponseBuilder respB = PivotQueryResponse.builder()
                .meta(Map.of(
                    "format", format.name(),
                    "templateId", request.getTemplateId(),
                    "templateVersionId", request.getTemplateVersionId()))
                .total(totalGroups);

            if (format == PivotOutputFormat.TABLE_ROWS) {
                // Columns: groupBy dims then measures (aliases)
                List<ColumnDto> columns = buildColumnsForTable(groupByDims, validatedMeasures, request.getTemplateId(), request.getTemplateVersionId());
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
            throw new PivotQueryException("Pivot execution failed: " + ex.getMessage(), ex);
        }
    }

    /* --------------------- helpers --------------------- */

    private List<FilterDto> convertFilters(List<FilterDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        return dtos.stream().map(fdto -> new FilterDto(fdto.field(), fdto.op(), fdto.value())).collect(Collectors.toList());
    }

    private List<SortDto> convertSorts(List<SortDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        return dtos.stream().map(sdto -> new SortDto(sdto.fieldOrAlias(), Boolean.TRUE.equals(sdto.desc()))).collect(Collectors.toList());
    }

    /**
     * Build columns list for TABLE_ROWS response.
     * Prefer friendly labels from metadataService when available.
     */
    private List<ColumnDto> buildColumnsForTable(List<String> groupByDims, List<ValidatedMeasure> measures, String templateId, String templateVersionId) {
        List<ColumnDto> cols = new ArrayList<>();

        // Attempt to fetch metadata for template to get labels
        PivotMetadataResponse meta = null;
        try {
            meta = metadataService.getMetadataForTemplate(templateId, templateVersionId);
        } catch (Exception ex) {
            log.debug("Unable to load metadata for template {}:{} - falling back to IDs", templateId, templateVersionId);
        }

        for (String dim : groupByDims) {
            String label = dim;
            if (meta != null) {
                label = findLabelFromMetadata(dim, meta).orElse(dim);
            }
            cols.add(ColumnDto.builder().id(dim).label(label).dataType(dim).build());
        }

        for (ValidatedMeasure vm : measures) {
            String id = vm.alias();
            String label = vm.alias();
            // try to get friendly name: validated measure may carry element id etc as metadata in extras — we try pivot metadata lookup
            if (meta != null && vm.etcUid() != null) {
                // etc: construct etc:<id> to match DTO id convention
                String etcClientId = "etc:" + vm.etcUid();
                Optional<PivotFieldDto> pf = meta.getMeasures().stream().filter(m -> etcClientId.equals(m.id())).findFirst();
                if (pf.isPresent()) label = pf.get().label();
            }
            cols.add(ColumnDto.builder().id(id).label(label).dataType(determineMeasureDataType(vm)).build());
        }

        return cols;
    }

    private Optional<String> findLabelFromMetadata(String id, PivotMetadataResponse meta) {
        if (meta == null) return Optional.empty();
        Stream<PivotFieldDto> all = Stream.concat(
            meta.getCoreDimensions() == null ? Stream.empty() : meta.getCoreDimensions().stream(),
            meta.getMeasures() == null ? Stream.empty() : meta.getMeasures().stream()
        );
        return all.filter(f -> id.equals(f.id())).map(PivotFieldDto::label).findFirst();
    }

    private String determineMeasureDataType(ValidatedMeasure vm) {
        // pick reasonable default data type name used by fieldMapper
        Field<?> tf = vm.targetField();
        if (tf == null) return "value_text";
        Class<?> javaType = tf.getDataType() != null ? tf.getDataType().getType() : tf.getType();
        if (Number.class.isAssignableFrom(javaType) || BigDecimal.class.isAssignableFrom(javaType)) return "value_num";
        if (Boolean.class.isAssignableFrom(javaType)) return "value_bool";
        if (java.time.LocalDateTime.class.isAssignableFrom(javaType)) return "value_ts";
        return "value_text";
    }

    /**
     * Map jOOQ Result<org.jooq.Record> to table rows. Each ColumnDto.id is used to pull value
     * from the org.jooq.Record (alias or column name).
     */
    private List<Map<String, Object>> mapResultToRows(Result<org.jooq.Record> result, List<ColumnDto> columns) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (org.jooq.Record r : result) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (ColumnDto c : columns) {
                Object val;
                try {
                    // Prefer typed get by alias/name
                    val = r.get(c.getId());
                } catch (Exception e) {
                    // fallback to safe retrieval
                    try {
                        val = r.getValue(c.getId());
                    } catch (Exception ex) {
                        val = null;
                    }
                }
                row.put(c.getId(), val);
            }
            rows.add(row);
        }
        return rows;
    }

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
            List<String> rowValues = rowDims.stream().map(d -> stringify(r.get(d))).collect(Collectors.toList());
            List<String> colValues = colDims.stream().map(d -> stringify(r.get(d))).collect(Collectors.toList());

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

    /* --------------------- Exception --------------------- */

    public static class PivotQueryException extends RuntimeException {
        public PivotQueryException(String msg) {
            super(msg);
        }

        public PivotQueryException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}

