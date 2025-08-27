package org.nmcpye.datarun.analytics.pivot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Select;
import org.nmcpye.datarun.analytics.pivot.dto.ColumnDto;
import org.nmcpye.datarun.analytics.pivot.dto.MeasureRequest;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryRequest;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryResponse;
import org.nmcpye.datarun.analytics.pivot.exception.InvalidMeasureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
public class PivotQueryService {

    private final PivotMetadataService pivotMetadataService;
    private final MeasureValidationService measureValidationService;
    private final PivotQueryBuilder pivotQueryBuilder;
    private final DSLContext dsl;

    // global hard cap to avoid accidental huge scans (tune as needed)
    private static final int HARD_LIMIT = 50_000;

    /**
     * Execute pivot query (template-mode).
     */
    @Transactional(readOnly = true)
    public PivotQueryResponse execute(PivotQueryRequest req, Set<String> allowedTeamIdsFromAuth) {
        Objects.requireNonNull(req, "PivotQueryRequest required");

        // enforce limits
        int limit = Optional.ofNullable(req.getLimit()).orElse(100);
        if (limit <= 0 || limit > HARD_LIMIT) throw new IllegalArgumentException("limit must be 1.." + HARD_LIMIT);
        int offset = Optional.ofNullable(req.getOffset()).orElse(0);

        // ACL teams: prefer explicit from request only after combining with auth derived allowedTeamIds
        Set<String> allowedTeamIds = allowedTeamIdsFromAuth;
        if (req.getAllowedTeamUids() != null && !req.getAllowedTeamUids().isEmpty()) {
            if (allowedTeamIds == null) allowedTeamIds = req.getAllowedTeamUids();
            else {
                // intersection: client cannot expand beyond authorization
                allowedTeamIds.retainAll(req.getAllowedTeamUids());
            }
        }

        // 1. validate measures and build ValidatedMeasure list
        List<ValidatedMeasure> validatedMeasures = new ArrayList<>();
        if (req.getMeasures() != null && !req.getMeasures().isEmpty()) {
            // detect duplicate aliases first
            Map<String, Long> aliasCounts = req.getMeasures().stream()
                .map(m -> Optional.ofNullable(m.getAlias()).orElse("").trim())
                .map(s -> s.isEmpty() ? null : s)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

            // we will either error or auto rename; default: error
            boolean autoRename = Boolean.TRUE.equals(req.getAutoRenameAliases());

            // Validate and optionally rename
            Map<String, AtomicInteger> aliasSeen = new HashMap<>();
            for (MeasureRequest mr : req.getMeasures()) {
                ValidatedMeasure vm = measureValidationService.validate(mr, req.getTemplateId(), req.getTemplateVersionId());

                // ensure alias
                String alias = mr.getAlias();
                if (alias == null || alias.isBlank()) {
                    alias = Objects.requireNonNull(vm.alias(), "validated alias missing");
                }
                // if duplicate and autoRename enabled, rename with numeric suffix
                if (aliasCounts.getOrDefault(alias, 0L) > 1) {
                    if (!autoRename) {
                        throw new InvalidMeasureException("Duplicate measure alias: " + alias + ". Set autoRenameAliases=true to allow automatic disambiguation.");
                    } else {
                        AtomicInteger ai = aliasSeen.computeIfAbsent(alias, k -> new AtomicInteger(0));
                        int n = ai.incrementAndGet();
                        if (n == 1) {
                            // first occurrence keep alias; subsequent will get suffix
                        } else {
                            alias = alias + "_" + n;
                        }
                        // produce a new ValidatedMeasure with alias replaced
                        vm = new ValidatedMeasure(vm.elementId(), vm.elementTemplateConfigId(), vm.aggregation(), vm.targetField(), vm.elementPredicate(), alias, vm.distinct(), vm.optionId(), vm.effectiveMode());
                    }
                }
                validatedMeasures.add(vm);
            }
        }

        // 2. transform filters DTOs to PivotQueryBuilder.Filter
        List<PivotQueryBuilder.Filter> filters = null;
        if (req.getFilters() != null) {
            filters = req.getFilters().stream()
                .map(f -> new PivotQueryBuilder.Filter(f.getField(), f.getOp(), f.getValue()))
                .collect(Collectors.toList());
        }

        // 3. transforms sorts
        List<PivotQueryBuilder.Sort> sorts = null;
        if (req.getSorts() != null) {
            sorts = req.getSorts().stream()
                .map(s -> new PivotQueryBuilder.Sort(s.getFieldOrAlias(), s.isDesc()))
                .collect(Collectors.toList());
        }

        // 4. Build and execute via builder
        Result<Record> result = pivotQueryBuilder.execute(
            req.getDimensions(),
            validatedMeasures,
            filters,
            req.getFrom(),
            req.getTo(),
            sorts,
            limit,
            offset,
            allowedTeamIds
        );

        // 5. Convert jOOQ Result -> List<Map<String,Object>>
        List<Map<String, Object>> rows = result.stream()
            .map(r -> {
                // r.intoMap() returns column name -> value, but keys may be in jOOQ's styling.
                return r.intoMap();
            }).collect(Collectors.toList());

        // 6. Build columns meta from record fields (first record or result.fields())
        List<org.jooq.Field<?>> fields = List.of(result.fields());
        List<ColumnDto> columns = Arrays.stream(result.fields())
            .map(f -> ColumnDto.builder().name(f.getName()).type(f.getDataType() == null ? "unknown" : f.getDataType().getType().getSimpleName()).source("unknown").build())
            .collect(Collectors.toList());

        long total = rows.size(); // note: is page size -- for full count you'd need separate count query

        return PivotQueryResponse.builder()
            .columns(columns)
            .rows(rows)
            .total(total)
            .meta(Map.of("mode", "template"))
            .build();
    }

    /**
     * Render query SQL only (inlined binds) for preview/debug.
     */
    @Transactional(readOnly = true)
    public String renderSql(PivotQueryRequest req, Set<String> allowedTeamIdsFromAuth) {
        // reuse logic to validate measures etc but don't execute
        // 1. validate measures (same steps as execute)
        List<ValidatedMeasure> validatedMeasures = new ArrayList<>();
        if (req.getMeasures() != null && !req.getMeasures().isEmpty()) {
            boolean autoRename = Boolean.TRUE.equals(req.getAutoRenameAliases());
            Map<String, Long> aliasCounts = req.getMeasures().stream()
                .map(m -> Optional.ofNullable(m.getAlias()).orElse("").trim())
                .map(s -> s.isEmpty() ? null : s)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

            Map<String, AtomicInteger> aliasSeen = new HashMap<>();
            for (MeasureRequest mr : req.getMeasures()) {
                ValidatedMeasure vm = measureValidationService.validate(mr, req.getTemplateId(), req.getTemplateVersionId());
                String alias = mr.getAlias();
                if (alias == null || alias.isBlank()) alias = vm.alias();
                if (aliasCounts.getOrDefault(alias, 0L) > 1) {
                    if (!autoRename) throw new InvalidMeasureException("Duplicate measure alias: " + alias);
                    AtomicInteger ai = aliasSeen.computeIfAbsent(alias, k -> new AtomicInteger(0));
                    int n = ai.incrementAndGet();
                    if (n > 1) alias = alias + "_" + n;
                    vm = new ValidatedMeasure(vm.elementId(), vm.elementTemplateConfigId(), vm.aggregation(), vm.targetField(), vm.elementPredicate(), alias, vm.distinct(), vm.optionId(), vm.effectiveMode());
                }
                validatedMeasures.add(vm);
            }
        }

        // transform filters + sorts as above
        List<PivotQueryBuilder.Filter> filters = req.getFilters() == null ? null : req.getFilters().stream()
            .map(f -> new PivotQueryBuilder.Filter(f.getField(), f.getOp(), f.getValue()))
            .collect(Collectors.toList());

        List<PivotQueryBuilder.Sort> sorts = req.getSorts() == null ? null : req.getSorts().stream()
            .map(s -> new PivotQueryBuilder.Sort(s.getFieldOrAlias(), s.isDesc()))
            .collect(Collectors.toList());

        Select<?> s = pivotQueryBuilder.buildSelect(
            req.getDimensions(),
            validatedMeasures,
            filters,
            req.getFrom(),
            req.getTo(),
            sorts,
            Optional.ofNullable(req.getLimit()).orElse(100),
            Optional.ofNullable(req.getOffset()).orElse(0),
            req.getAllowedTeamUids() == null ? allowedTeamIdsFromAuth : req.getAllowedTeamUids()
        );

        // inlined SQL (use with caution for logs/debug only)
        return dsl.renderInlined(s);
    }
}
