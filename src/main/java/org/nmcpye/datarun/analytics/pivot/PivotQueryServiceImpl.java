package org.nmcpye.datarun.analytics.pivot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Result;
import org.jooq.Select;
import org.nmcpye.datarun.analytics.pivot.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that validates measure requests, builds the query via PivotQueryBuilder,
 * executes it, and returns rows as List<Map<String,Object>>. Template-mode-first.
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PivotQueryServiceImpl implements PivotQueryService {
    private final MeasureValidationService measureValidationService;
    private final PivotMetadataService pivotMetadataService;
    private final PivotQueryBuilder queryBuilder;
    private final DSLContext dsl;
    private final PivotFieldJooqMapper fieldMapper;

    @Override
    public PivotQueryResponse query(PivotQueryRequest request, PivotOutputFormat format,
                                    Set<String> allowedTeamIdsFromAuth) {
        Objects.requireNonNull(request, "request required");

        // 1) Validate and build measures
        List<ValidatedMeasure> vms = new ArrayList<>();
        if (request.getMeasures() != null) {
            for (MeasureRequest mr : request.getMeasures()) {
                ValidatedMeasure vm = measureValidationService.validate(mr, request.getTemplateId(), request.getTemplateVersionId());
                vms.add(vm);
            }
        }

        // 2) Build filters
        List<PivotQueryBuilder.Filter> filters = null;
        if (request.getFilters() != null) {
            filters = request.getFilters().stream()
                .map(f -> new PivotQueryBuilder.Filter(f.getField(), f.getOp(), f.getValue()))
                .collect(Collectors.toList());
        }

        // 3) Determine grouping dimensions: prefer explicit row/column dims
        List<String> rowDims = request.getRowDimensions();
        List<String> colDims = request.getColumnDimensions();
        List<String> groupingDims = new ArrayList<>();

        if (rowDims != null && !rowDims.isEmpty()) {
            groupingDims.addAll(rowDims);
        }
        if (colDims != null && !colDims.isEmpty()) {
            groupingDims.addAll(colDims);
        }

        // Backwards compatibility: if no explicit row/col, fall back to generic dimensions list
        if (groupingDims.isEmpty() && request.getDimensions() != null && !request.getDimensions().isEmpty()) {
            groupingDims.addAll(request.getDimensions());
            // If the UI provided a single list but we will serve PIVOT_MATRIX, it expects at least two dims:
            // We'll still allow it — the PIVOT_MATRIX logic falls back to first/second dims.
            // Clients that send dimensions only: still supported (we use dimensions as fallback).
            // For PIVOT_MATRIX, dimensions[0] → row, dimensions[1] → column (if present).
            //
            // Preferred new API: use rowDimensions and columnDimensions explicitly. This enables multi-dimension concatenation later.
            // SQL grouping: we group by all row + column dims (so you get deterministic grouping).
        }

        // 4) ACL allowed ids
        Set<String> allowedTeamIds = request.getAllowedTeamUids();

        // 5) Build select using groupingDims (these will become our GROUP BY columns)
        Select<org.jooq.Record> sel = queryBuilder.buildSelect(
            groupingDims,
            vms,
            filters,
            request.getFrom(),
            request.getTo(),
            // map sorts as before
            request.getSorts() == null ? null :
                request.getSorts().stream()
                    .map(s -> new PivotQueryBuilder.Sort(s.getFieldOrAlias(), s.isDesc()))
                    .collect(Collectors.toList()),
            request.getLimit(),
            request.getOffset(),
            allowedTeamIds
        );

        Result<org.jooq.Record> rows = dsl.fetch(sel);

        // 6) Convert to requested format — note: PIVOT_MATRIX now expects explicit row/column dims if present
        if (format == PivotOutputFormat.TABLE_ROWS) {
            return toTableRowsResponse(rows, request, vms);
        } else {
            // decide effective row & column dims for matrix:
            List<String> effectiveRowDims;
            List<String> effectiveColDims;

            if (rowDims != null && !rowDims.isEmpty()) effectiveRowDims = rowDims;
            else if (request.getDimensions() != null && !request.getDimensions().isEmpty())
                effectiveRowDims = List.of(request.getDimensions().get(0));
            else effectiveRowDims = List.of();

            if (colDims != null && !colDims.isEmpty()) effectiveColDims = colDims;
            else if (request.getDimensions() != null && request.getDimensions().size() > 1)
                effectiveColDims = List.of(request.getDimensions().get(1));
            else effectiveColDims = List.of();

            // call a new matrix builder that accepts lists (so we can support multi-dim concatenation later)
            return toPivotMatrixResponse(rows, request, vms, effectiveRowDims, effectiveColDims);
        }
    }

    @Override
    public long count(PivotQueryRequest request) {
        // Build same select but replace projection with count(*) over groups.
        // Simpler approach: reuse the builder and transform to count query.
        List<ValidatedMeasure> vms = new ArrayList<>();
        if (request.getMeasures() != null) {
            for (MeasureRequest mr : request.getMeasures()) {
                vms.add(measureValidationService.validate(mr, request.getTemplateId(), request.getTemplateVersionId()));
            }
        }
        List<PivotQueryBuilder.Filter> filters = null;
        if (request.getFilters() != null) {
            filters = request.getFilters().stream().map(f -> new PivotQueryBuilder.Filter(f.getField(), f.getOp(), f.getValue())).toList();
        }

        Select<org.jooq.Record> sel = queryBuilder.buildSelect(request.getDimensions(), vms, filters,
            request.getFrom(), request.getTo(), null, request.getLimit(), request.getOffset(), request.getAllowedTeamUids());

        // wrap as a count of rows returned by the select (subquery)
        var cnt = dsl.selectCount().from(sel.asTable("t_count")).fetchOne(0, Long.class);
        return cnt == null ? 0L : cnt;
    }

    // ---------- helpers ----------

    private PivotQueryResponse toTableRowsResponse(Result<org.jooq.Record> rows, PivotQueryRequest req, List<ValidatedMeasure> vms) {
        List<ColumnDto> columns = new ArrayList<>();
        // columns == requested dimensions (as-is) + measure aliases
        if (req.getDimensions() != null) {
            for (String d : req.getDimensions()) {
                columns.add(ColumnDto.builder().name(d).type(determineDimensionType(d)).source("dimension").build());
            }
        }
        if (vms != null) {
            for (ValidatedMeasure vm : vms) {
                columns.add(ColumnDto.builder().name(vm.alias()).type(determineMeasureType(vm)).source("measure").build());
            }
        }

        List<Map<String, Object>> outRows = new ArrayList<>();
        for (org.jooq.Record r : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (ColumnDto c : columns) {
                // prefer typed access if possible
                Object val;
                try {
                    val = r.get(c.getName());
                } catch (Exception ex) {
                    // fallback by trying the field name case-insensitively
                    val = r.getValue(c.getName());
                }
                map.put(c.getName(), val);
            }
            outRows.add(map);
        }

        long total = rows.size(); // if pagination, the client might call /count separately
        return PivotQueryResponse.builder().columns(columns).rows(outRows).total(total).meta(Map.of("format", "TABLE_ROWS")).build();
    }

    /**
     * Very small pivot matrix implementation:
     * - requires at least 2 dimensions: [rowDim, colDim, ...]
     * - measures: one or more measures; for each (rowKey,colKey) we collect measure aliases
     * - output columns: [ rowDim, <col-key>-<measure-alias>... ]
     * <p>
     * This is intentionally conservative and in-memory. For very large result sets,
     * move pivot logic into DB or stream the result.
     * <p>
     * updated to accept lists of row/col dims and produce composite keys
     * (so multi-dim joins are possible later).
     */
    private PivotQueryResponse toPivotMatrixResponse(Result<org.jooq.Record> rows,
                                                     PivotQueryRequest req,
                                                     List<ValidatedMeasure> vms,
                                                     List<String> effectiveRowDims,
                                                     List<String> effectiveColDims) {
        // Must have at least 1 rowDim and 1 colDim
        if (effectiveRowDims == null || effectiveRowDims.isEmpty() ||
            effectiveColDims == null || effectiveColDims.isEmpty()) {
            throw new IllegalArgumentException("PIVOT_MATRIX requires at least one row dimension and one column dimension");
        }

        // build row key/composite by concatenating row dimension values with '||' separator
        String rowKeySep = "||";
        String colKeySep = "||";

        LinkedHashSet<String> colKeys = new LinkedHashSet<>();
        Map<String, Map<String, Map<String, Object>>> matrix = new LinkedHashMap<>();

        for (org.jooq.Record rec : rows) {
            // build composite row key
            StringBuilder rb = new StringBuilder();
            for (int i = 0; i < effectiveRowDims.size(); i++) {
                if (i > 0) rb.append(rowKeySep);
                Object v = rec.get(effectiveRowDims.get(i));
                rb.append(v == null ? "NULL" : v.toString());
            }
            String rkey = rb.toString();

            // build composite col key
            StringBuilder cb = new StringBuilder();
            for (int i = 0; i < effectiveColDims.size(); i++) {
                if (i > 0) cb.append(colKeySep);
                Object v = rec.get(effectiveColDims.get(i));
                cb.append(v == null ? "NULL" : v.toString());
            }
            String ckey = cb.toString();

            colKeys.add(ckey);
            matrix.computeIfAbsent(rkey, k -> new LinkedHashMap<>())
                .computeIfAbsent(ckey, k -> new LinkedHashMap<>());

            for (ValidatedMeasure vm : vms) {
                Object mv = rec.get(vm.alias());
                matrix.get(rkey).get(ckey).put(vm.alias(), mv);
            }
        }

        // Build columns: first column maps to the concatenated row dims label
        List<ColumnDto> columns = new ArrayList<>();
        String rowDimName = String.join("_", effectiveRowDims);
        columns.add(ColumnDto.builder().name(rowDimName).type(determineDimensionType(effectiveRowDims.get(0))).source("dimension").build());

        List<String> colKeyList = new ArrayList<>(colKeys);
        for (String ckey : colKeyList) {
            for (ValidatedMeasure vm : vms) {
                String cname = ckey + "__" + vm.alias();
                columns.add(ColumnDto.builder().name(cname).type(determineMeasureType(vm)).source("measure").build());
            }
        }

        // rows
        List<Map<String, Object>> outRows = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> rowEntry : matrix.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(rowDimName, rowEntry.getKey());
            Map<String, Map<String, Object>> cols = rowEntry.getValue();
            for (String ckey : colKeyList) {
                Map<String, Object> measuresPerCell = cols.getOrDefault(ckey, Map.of());
                for (ValidatedMeasure vm : vms) {
                    String cname = ckey + "__" + vm.alias();
                    map.put(cname, measuresPerCell.get(vm.alias()));
                }
            }
            outRows.add(map);
        }

        long total = outRows.size();
        return PivotQueryResponse.builder()
            .columns(columns)
            .rows(outRows)
            .total(total)
            .meta(Map.of("format", "PIVOT_MATRIX",
                "rowDimensions", effectiveRowDims,
                "columnDimensions", effectiveColDims))
            .build();
    }

    private String determineDimensionType(String dim) {
        // simple mapping; expand if needed or consult metadata service
        return switch (dim) {
            case "team_id", "org_unit_id", "activity_id", "element_id", "option_id" -> "STRING";
            case "submission_completed_at" -> "TIMESTAMP";
            default -> "STRING";
        };
    }

    private String determineMeasureType(ValidatedMeasure vm) {
        // lightweight: ask the targetField dataType where possible
        if (vm.targetField() == null) return "STRING";
        DataType<?> dt = vm.targetField().getDataType();
        if (dt != null) {
            Class<?> t = dt.getType();
            if (Number.class.isAssignableFrom(t) || BigDecimal.class.equals(t)) return "NUMBER";
            if (Boolean.class.equals(t)) return "BOOLEAN";
            if (java.time.LocalDateTime.class.equals(t) || java.sql.Timestamp.class.equals(t)) return "TIMESTAMP";
        }
        return "STRING";
    }


//    /**
//     * Execute pivot query (template-mode).
//     */
//    @Transactional(readOnly = true)
//    public PivotQueryResponse execute(PivotQueryRequest req, Set<String> allowedTeamIdsFromAuth) {
//        Objects.requireNonNull(req, "PivotQueryRequest required");
//
//        // enforce limits
//        int limit = Optional.ofNullable(req.getLimit()).orElse(100);
//        if (limit <= 0 || limit > HARD_LIMIT) throw new IllegalArgumentException("limit must be 1.." + HARD_LIMIT);
//        int offset = Optional.ofNullable(req.getOffset()).orElse(0);
//
//        // ACL teams: prefer explicit from request only after combining with auth derived allowedTeamIds
//        Set<String> allowedTeamIds = allowedTeamIdsFromAuth;
//        if (req.getAllowedTeamUids() != null && !req.getAllowedTeamUids().isEmpty()) {
//            if (allowedTeamIds == null) allowedTeamIds = req.getAllowedTeamUids();
//            else {
//                // intersection: client cannot expand beyond authorization
//                allowedTeamIds.retainAll(req.getAllowedTeamUids());
//            }
//        }
//
//        // 1. validate measures and build ValidatedMeasure list
//        List<ValidatedMeasure> validatedMeasures = new ArrayList<>();
//        if (req.getMeasures() != null && !req.getMeasures().isEmpty()) {
//            // detect duplicate aliases first
//            Map<String, Long> aliasCounts = req.getMeasures().stream()
//                .map(m -> Optional.ofNullable(m.getAlias()).orElse("").trim())
//                .map(s -> s.isEmpty() ? null : s)
//                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
//
//            // we will either error or auto rename; default: error
//            boolean autoRename = Boolean.TRUE.equals(req.getAutoRenameAliases());
//
//            // Validate and optionally rename
//            Map<String, AtomicInteger> aliasSeen = new HashMap<>();
//            for (MeasureRequest mr : req.getMeasures()) {
//                ValidatedMeasure vm = measureValidationService.validate(mr, req.getTemplateId(), req.getTemplateVersionId());
//
//                // ensure alias
//                String alias = mr.getAlias();
//                if (alias == null || alias.isBlank()) {
//                    alias = Objects.requireNonNull(vm.alias(), "validated alias missing");
//                }
//                // if duplicate and autoRename enabled, rename with numeric suffix
//                if (aliasCounts.getOrDefault(alias, 0L) > 1) {
//                    if (!autoRename) {
//                        throw new InvalidMeasureException("Duplicate measure alias: " + alias + ". Set autoRenameAliases=true to allow automatic disambiguation.");
//                    } else {
//                        AtomicInteger ai = aliasSeen.computeIfAbsent(alias, k -> new AtomicInteger(0));
//                        int n = ai.incrementAndGet();
//                        if (n == 1) {
//                            // first occurrence keep alias; subsequent will get suffix
//                        } else {
//                            alias = alias + "_" + n;
//                        }
//                        // produce a new ValidatedMeasure with alias replaced
//                        vm = new ValidatedMeasure(vm.elementId(), vm.elementTemplateConfigId(), vm.aggregation(), vm.targetField(), vm.elementPredicate(), alias, vm.distinct(), vm.optionId(), vm.effectiveMode());
//                    }
//                }
//                validatedMeasures.add(vm);
//            }
//        }
//
//        // 2. transform filters DTOs to PivotQueryBuilder.Filter
//        List<PivotQueryBuilder.Filter> filters = null;
//        if (req.getFilters() != null) {
//            filters = req.getFilters().stream()
//                .map(f -> new PivotQueryBuilder.Filter(f.getField(), f.getOp(), f.getValue()))
//                .collect(Collectors.toList());
//        }
//
//        // 3. transforms sorts
//        List<PivotQueryBuilder.Sort> sorts = null;
//        if (req.getSorts() != null) {
//            sorts = req.getSorts().stream()
//                .map(s -> new PivotQueryBuilder.Sort(s.getFieldOrAlias(), s.isDesc()))
//                .collect(Collectors.toList());
//        }
//
//        // 4. Build and execute via builder
//        Result<org.jooq.Record> result = pivotQueryBuilder.execute(
//            req.getDimensions(),
//            validatedMeasures,
//            filters,
//            req.getFrom(),
//            req.getTo(),
//            sorts,
//            limit,
//            offset,
//            allowedTeamIds
//        );
//
//        // 5. Convert jOOQ Result -> List<Map<String,Object>>
//        List<Map<String, Object>> rows = result.stream()
//            .map(r -> {
//                // r.intoMap() returns column name -> value, but keys may be in jOOQ's styling.
//                return r.intoMap();
//            }).collect(Collectors.toList());
//
//        // 6. Build columns meta from record fields (first record or result.fields())
//        List<org.jooq.Field<?>> fields = List.of(result.fields());
//        List<ColumnDto> columns = Arrays.stream(result.fields())
//            .map(f -> ColumnDto.builder().name(f.getName()).type(f.getDataType() == null ? "unknown" : f.getDataType().getType().getSimpleName()).source("unknown").build())
//            .collect(Collectors.toList());
//
//        long total = rows.size(); // note: is page size -- for full count you'd need separate count query
//
//        return PivotQueryResponse.builder()
//            .columns(columns)
//            .rows(rows)
//            .total(total)
//            .meta(Map.of("mode", "template"))
//            .build();
//    }
//
//    /**
//     * Render query SQL only (inlined binds) for preview/debug.
//     */
//    @Transactional(readOnly = true)
//    public String renderSql(PivotQueryRequest req, Set<String> allowedTeamIdsFromAuth) {
//        // reuse logic to validate measures etc but don't execute
//        // 1. validate measures (same steps as execute)
//        List<ValidatedMeasure> validatedMeasures = new ArrayList<>();
//        if (req.getMeasures() != null && !req.getMeasures().isEmpty()) {
//            boolean autoRename = Boolean.TRUE.equals(req.getAutoRenameAliases());
//            Map<String, Long> aliasCounts = req.getMeasures().stream()
//                .map(m -> Optional.ofNullable(m.getAlias()).orElse("").trim())
//                .map(s -> s.isEmpty() ? null : s)
//                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
//
//            Map<String, AtomicInteger> aliasSeen = new HashMap<>();
//            for (MeasureRequest mr : req.getMeasures()) {
//                ValidatedMeasure vm = measureValidationService.validate(mr, req.getTemplateId(), req.getTemplateVersionId());
//                String alias = mr.getAlias();
//                if (alias == null || alias.isBlank()) alias = vm.alias();
//                if (aliasCounts.getOrDefault(alias, 0L) > 1) {
//                    if (!autoRename) throw new InvalidMeasureException("Duplicate measure alias: " + alias);
//                    AtomicInteger ai = aliasSeen.computeIfAbsent(alias, k -> new AtomicInteger(0));
//                    int n = ai.incrementAndGet();
//                    if (n > 1) alias = alias + "_" + n;
//                    vm = new ValidatedMeasure(vm.elementId(), vm.elementTemplateConfigId(), vm.aggregation(), vm.targetField(), vm.elementPredicate(), alias, vm.distinct(), vm.optionId(), vm.effectiveMode());
//                }
//                validatedMeasures.add(vm);
//            }
//        }
//
//        // transform filters + sorts as above
//        List<PivotQueryBuilder.Filter> filters = req.getFilters() == null ? null : req.getFilters().stream()
//            .map(f -> new PivotQueryBuilder.Filter(f.getField(), f.getOp(), f.getValue()))
//            .collect(Collectors.toList());
//
//        List<PivotQueryBuilder.Sort> sorts = req.getSorts() == null ? null : req.getSorts().stream()
//            .map(s -> new PivotQueryBuilder.Sort(s.getFieldOrAlias(), s.isDesc()))
//            .collect(Collectors.toList());
//
//        Select<?> s = pivotQueryBuilder.buildSelect(
//            req.getDimensions(),
//            validatedMeasures,
//            filters,
//            req.getFrom(),
//            req.getTo(),
//            sorts,
//            Optional.ofNullable(req.getLimit()).orElse(100),
//            Optional.ofNullable(req.getOffset()).orElse(0),
//            req.getAllowedTeamUids() == null ? allowedTeamIdsFromAuth : req.getAllowedTeamUids()
//        );
//
//        // inlined SQL (use with caution for logs/debug only)
//        return dsl.renderInlined(s);
//    }
}
