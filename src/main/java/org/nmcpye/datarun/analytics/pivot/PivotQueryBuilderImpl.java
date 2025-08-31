package org.nmcpye.datarun.analytics.pivot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.analytics.pivot.dto.FilterDto;
import org.nmcpye.datarun.analytics.pivot.dto.SortDto;
import org.nmcpye.datarun.jooq.Tables;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PivotQueryBuilder builds a grouped pivot query against the materialized view / facts (pivot_grid_facts).
 * <p>
 * Key ideas:
 * - Accepts validated measures (ValidatedMeasure) produced by MeasureValidationService.
 * - Uses SelectQuery to avoid compile-time fluent-type fights with jOOQ.
 * - Coerces filter values to the Field's Java type at runtime (via field.getDataType().getType()).
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PivotQueryBuilderImpl {

    private final DSLContext dsl;

    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;

    /**
     * Build the jOOQ Select query (without fetching).
     * Ensures deterministic ordering by adding a group-aware tie-breaker (MIN(value_id)).
     */
    public Select<Record> buildSelect(
        List<String> dimensions,
        List<ValidatedMeasure> measures,
        List<FilterDto> filters,
        LocalDateTime from,
        LocalDateTime to,
        List<SortDto> sorts,
        Integer limit,
        Integer offset,
        Set<String> allowedTeamIds
    ) {
        List<Field<?>> select = new ArrayList<>();
        List<Field<?>> groupBy = new ArrayList<>();
        Map<String, Field<?>> aliasToSelectField = new HashMap<>();

        // dimensions -> groupBy/select
        if (dimensions != null) {
            for (String dim : dimensions) {
                Field<?> f = resolveFactField(dim);
                select.add(f);
                groupBy.add(f);
            }
        }

        // measures -> aggregates
        if (measures != null) {
            for (ValidatedMeasure vm : measures) {
                Field<?> agg = buildAggregateField(vm);
                select.add(agg);
                aliasToSelectField.put(vm.alias(), agg);
            }
        }

        if (select.isEmpty()) {
            select.add(DSL.count().as("count"));
        }

        SelectQuery<Record> query = dsl.selectQuery();
        query.addSelect(select.toArray(new SelectField<?>[0]));
        query.addFrom(PG);

        // build base condition (reuse your translateFilter & coerce logic)
        Condition cond = PG.DELETED_AT.isNull();
        if (from != null) cond = cond.and(PG.SUBMISSION_COMPLETED_AT.ge(from));
        if (to   != null) cond = cond.and(PG.SUBMISSION_COMPLETED_AT.le(to));
        if (allowedTeamIds != null && !allowedTeamIds.isEmpty()) cond = cond.and(PG.TEAM_UID.in(allowedTeamIds));

        if (filters != null) {
            for (var fil : filters) {
                Field<?> f = resolveFactFieldOrAlias(fil.field(), aliasToSelectField);
                cond = cond.and(translateFilter(f, fil.op(), fil.value()));
            }
        }

        query.addConditions(cond);

        // groupBy if any
        if (!groupBy.isEmpty()) {
            query.addGroupBy(groupBy.toArray(new Field<?>[0]));
        }

        // ---- Sorting / tie-breaker logic ----
        List<SortField<?>> sortFields = new ArrayList<>();
        if (sorts != null && !sorts.isEmpty()) {
            for (var s : sorts) {
                Field<?> candidate = aliasToSelectField.getOrDefault(s.fieldOrAlias(), resolveFactField(s.fieldOrAlias()));
                // candidate might be a Field<?> or aggregate alias; calling asc()/desc() is valid on Field<?>
                sortFields.add(s.desc() ? candidate.desc() : candidate.asc());
            }
        } else {
            // default: if grouped, default to groupBy fields (asc)
            if (!groupBy.isEmpty()) {
                for (Field<?> g : groupBy) sortFields.add(g.asc());
            }
        }

        // Always append a deterministic grouped tie-breaker when grouping: MIN(value_id) ASC
        // This is an aggregate expression and therefore legal in GROUP BY queries.
        if (!groupBy.isEmpty()) {
            // Use MIN(value_id) as stable tiebreaker per group
            SortField<?> tie = DSL.min(PG.VALUE_ID).asc();
            sortFields.add(tie);
        } else {
            // No grouping -> don't add MIN(value_id). If no explicit sort provided, we keep no ORDER BY
            // to avoid ordering by non-aggregate columns when only aggregates are selected.
        }

        if (!sortFields.isEmpty()) {
            query.addOrderBy(sortFields.toArray(new SortField<?>[0]));
        }

        // pagination
        query.addLimit(limit == null ? 100 : limit);
        if (offset != null) query.addOffset(offset);

        return query;
    }

    /**
     * Count number of groups for the requested grouping/filter set.
     * If groupBy dims are empty, return 1 when there are matching rows, otherwise 0.
     */
    public long countGroups(
        List<String> dimensions,
        List<ValidatedMeasure> measures,
        List<FilterDto> filters,
        LocalDateTime from,
        LocalDateTime to,
        Set<String> allowedTeamIds
    ) {
        // Build the same base condition
        Condition cond = PG.DELETED_AT.isNull();
        if (from != null) cond = cond.and(PG.SUBMISSION_COMPLETED_AT.ge(from));
        if (to != null)   cond = cond.and(PG.SUBMISSION_COMPLETED_AT.le(to));
        if (allowedTeamIds != null && !allowedTeamIds.isEmpty()) cond = cond.and(PG.TEAM_UID.in(allowedTeamIds));

        // translate filters (no alias map necessary here)
        if (filters != null) {
            for (var fil : filters) {
                Field<?> f = resolveFactField(fil.field());
                cond = cond.and(translateFilter(f, fil.op(), fil.value()));
            }
        }

        // group fields
        List<Field<?>> groupBy = new ArrayList<>();
        if (dimensions != null) {
            for (String dim : dimensions) groupBy.add(resolveFactField(dim));
        }

        if (groupBy.isEmpty()) {
            // Global aggregate => 1 group if any matching row, else 0
            Integer exists = dsl.select(DSL.one()).from(PG).where(cond).limit(1).fetchOne(0, Integer.class);
            return (exists != null) ? 1L : 0L;
        } else {
            // Build a grouped subselect and COUNT(*) over it
            Select<Record> grouped = dsl.select(groupBy.toArray(new Field<?>[0])).from(PG).where(cond).groupBy(groupBy.toArray(new Field<?>[0]));
            Table<?> sub = grouped.asTable("g");
            Long cnt = dsl.selectCount().from(sub).fetchOne(0, Long.class);
            return cnt == null ? 0L : cnt;
        }
    }


    /**
     * Execute a pivot query (template-mode oriented).
     *
     * @param dimensions     list of column names to group by (must match pivot_grid_facts column names, e.g. "team_id", "element_id")
     * @param measures       already validated and typed measures (ValidatedMeasure)
     * @param filters        additional filters on facts (field/op/value). Field may be dimension/measure column or alias.
     * @param from           inclusive lower bound for submission_completed_at
     * @param to             inclusive upper bound for submission_completed_at
     * @param sorts          sort ordering (by alias or by dimension name)
     * @param limit          page limit
     * @param offset         page offset
     * @param allowedTeamIds optional ACL filter for team_id
     * @return Result<Record> rows
     */
    public Result<Record> execute(
        List<String> dimensions,
        List<ValidatedMeasure> measures,
        List<FilterDto> filters,
        LocalDateTime from,
        LocalDateTime to,
        List<SortDto> sorts,
        Integer limit,
        Integer offset,
        Set<String> allowedTeamIds
    ) {
        var query = buildSelect(dimensions, measures, filters, from, to, sorts, limit, offset, allowedTeamIds);
        return query.fetch();
    }

    /**
     * Build aggregate select field for a given validated measure (safe casts).
     */
    @SuppressWarnings("unchecked")
    private Field<?> buildAggregateField(ValidatedMeasure vm) {
        // alias
        String alias = vm.alias();

        // elements predicate to scope aggregate (vm.elementPredicate())
        Condition scope = vm.elementPredicate() == null ? DSL.noCondition() : vm.elementPredicate();

        switch (vm.aggregation()) {
            case SUM -> {
                // sum numeric
                Field<BigDecimal> num = (Field<BigDecimal>) vm.targetField();
                return DSL.sum(num).filterWhere(scope).as(alias);
            }
            case AVG -> {
                Field<BigDecimal> num = (Field<BigDecimal>) vm.targetField();
                return DSL.avg(num).filterWhere(scope).as(alias);
            }
            case MIN -> {
                // min works on multiple types; rely on targetField type
                return DSL.min(vm.targetField()).filterWhere(scope).as(alias);
            }
            case MAX -> {
                return DSL.max(vm.targetField()).filterWhere(scope).as(alias);
            }
            case COUNT -> {
                if (vm.distinct()) {
                    return DSL.countDistinct(vm.targetField()).filterWhere(scope).as(alias);
                } else {
                    // plain count of rows matching predicate
                    return DSL.count().filterWhere(scope).as(alias);
                }
            }
            case COUNT_DISTINCT -> {
                return DSL.countDistinct(vm.targetField()).filterWhere(scope).as(alias);
            }
            case SUM_TRUE -> {
                // targetField is expected to be an integer CASE expr or boolean field mapped to CASE
                Field<Integer> expr = (Field<Integer>) vm.targetField();
                return DSL.sum(expr).filterWhere(scope).as(alias);
            }
            default -> {
                // fallback
                return DSL.count().as(alias);
            }
        }
    }

    /**
     * Resolve known pivot_grid_facts field by name. Use typed generated fields
     * when possible. If unknown, produce a typed DSL.field using Object.class as fallback.
     */
    private Field<?> resolveFactField(String column) {
        if (column == null) return DSL.noField();

        return switch (column) {
            case "team_uid" -> PG.TEAM_UID;
            case "org_unit_uid" -> PG.ORG_UNIT_UID;
            case "activity_uid" -> PG.ACTIVITY_UID;
            case "de_uid" -> PG.DE_UID;
            case "option_uid" -> PG.OPTION_UID;
            case "value_num" -> PG.VALUE_NUM;
            case "value_text" -> PG.VALUE_TEXT;
            case "value_bool" -> PG.VALUE_BOOL;
            case "value_ts" -> PG.VALUE_TS;
            case "submission_uid" -> PG.SUBMISSION_UID;
            case "submission_completed_at" -> PG.SUBMISSION_COMPLETED_AT;
            case "repeat_instance_id" -> PG.REPEAT_INSTANCE_ID;
            case "category_uid" -> PG.CHILD_CATEGORY_UID;
            case "etc_uid" -> PG.ETC_UID;
            case "value_id", "id" -> PG.VALUE_ID;               // <--- add mapping for the stable PK
            default -> {
                // Best-effort typed field fallback
                yield DSL.field(DSL.name(column), Object.class);
            }
        };
    }

    /**
     * Resolve either an alias (an already built SelectField) or a fact field.
     * <p>
     * IMPORTANT: The aliasMap parameter should accept SelectField<?> values. Use
     * Map<String, ? extends Field<?>> when passing such a map to avoid generics mismatch.
     */
    private Field<?> resolveFactFieldOrAlias(String nameOrAlias, Map<String, Field<?>> aliasMap) {
        if (nameOrAlias == null) return DSL.noField();

        if (aliasMap != null) {
            Field<?> f = aliasMap.get(nameOrAlias);
            if (f != null) return f; // SelectField<?> is a Field<?>, safe
        }
        return resolveFactField(nameOrAlias);
    }

    /**
     * Translate a filter into a typed jOOQ Condition.
     * Uses DSL.val(value, dataType) to ensure correct binding type.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Condition translateFilter(Field<?> f, String op, Object value) {
        if (f == null) throw new IllegalArgumentException("Filter field is required");
        if (op == null) throw new IllegalArgumentException("Operator is required");

        DataType<?> dataType = f.getDataType();
        Class<?> targetClass = dataType != null ? dataType.getType() : String.class;

        // IN semantics
        if ("IN".equalsIgnoreCase(op)) {
            if (!(value instanceof Collection<?>)) {
                throw new IllegalArgumentException("IN operator requires a collection value");
            }
            Collection<?> rawColl = (Collection<?>) value;
            if (rawColl.isEmpty()) {
                // Policy: empty IN -> match nothing
                return DSL.falseCondition();
            }

            List<Object> coerced = rawColl.stream()
                .map(v -> coerceToType(v, targetClass))
                .collect(Collectors.toList());

            if (dataType != null) {
                // create Field<T>[] using the target DataType so jOOQ binds properly
                @SuppressWarnings("unchecked")
                Field<Object>[] fieldVals = coerced.stream()
                    .map(v -> (Field<Object>) DSL.val(v, (DataType) dataType))
                    .toArray(Field[]::new);
                // use varargs in(...) overload
                return ((Field) f).in(fieldVals);
            } else {
                // fallback: pass raw coerced values
                return ((Field) f).in(coerced);
            }
        }

        // NULL handling for single-value operators
        if (value == null) {
            switch (op) {
                case "=":
                case "==":
                    return ((Field) f).isNull();
                case "!=":
                case "<>":
                    return ((Field) f).isNotNull();
                default:
                    throw new IllegalArgumentException("Operator " + op + " not supported for NULL value");
            }
        }

        // Coerce single value
        Object coerced = coerceToType(value, targetClass);
        Field<?> right = dataType != null ? DSL.val(coerced, (DataType) dataType) : DSL.val(coerced);

        switch (op) {
            case "=":
            case "==":
                return ((Field) f).eq(right);
            case "!=":
            case "<>":
                return ((Field) f).ne(right);
            case ">":
                return ((Field) f).gt(right);
            case "<":
                return ((Field) f).lt(right);
            case ">=":
                return ((Field) f).ge(right);
            case "<=":
                return ((Field) f).le(right);
            case "LIKE":
                if (!String.class.equals(targetClass) && !CharSequence.class.isAssignableFrom(targetClass)) {
                    throw new IllegalArgumentException("LIKE is only valid on string fields");
                }
                return ((Field<String>) f).like(coerced.toString());
            case "ILIKE":
                if (!String.class.equals(targetClass) && !CharSequence.class.isAssignableFrom(targetClass)) {
                    throw new IllegalArgumentException("ILIKE is only valid on string fields");
                }
                // Use SQL ILIKE where supported (Postgres). jOOQ will render appropriately.
                return ((Field<String>) f).likeIgnoreCase(coerced.toString());
            default:
                throw new IllegalArgumentException("Unsupported operator: " + op);
        }
    }

    /**
     * Coerce a value to the given target Java type.
     * Conservative: throws IllegalArgumentException for unsupported conversions.
     */
    private Object coerceToType(Object raw, Class<?> target) {
        if (raw == null) return null;
        if (target == null) return raw;
        if (target.isInstance(raw)) return raw;

        String s = raw.toString().trim();

        // Numeric targets
        if (BigDecimal.class.equals(target) || Number.class.equals(target) || Number.class.isAssignableFrom(target)) {
            try {
                BigDecimal bd = new BigDecimal(s);
                // If target expects a narrower type, convert:
                if (Integer.class.equals(target) || int.class.equals(target)) return bd.intValueExact();
                if (Long.class.equals(target) || long.class.equals(target)) return bd.longValueExact();
                if (Double.class.equals(target) || double.class.equals(target)) return bd.doubleValue();
                // Default: return BigDecimal for numeric-like targets
                return bd;
            } catch (ArithmeticException | NumberFormatException ex) {
                throw new IllegalArgumentException("Cannot coerce to numeric: " + s, ex);
            }
        }

        // Boolean
        if (Boolean.class.equals(target) || boolean.class.equals(target)) {
            if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) return Boolean.TRUE;
            if ("0".equals(s) || "false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) return Boolean.FALSE;
            throw new IllegalArgumentException("Cannot coerce to boolean: " + s);
        }

        // LocalDateTime / Timestamp
        if (LocalDateTime.class.equals(target) || Timestamp.class.equals(target)) {
            // Accept epoch millis or ISO strings
            try {
                if (s.matches("\\d+")) {
                    long epoch = Long.parseLong(s);
                    LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
                    if (Timestamp.class.equals(target)) return Timestamp.valueOf(ldt);
                    return ldt;
                } else {
                    try {
                        Instant inst = Instant.parse(s); // "2025-08-14T..."
                        LocalDateTime ldt = LocalDateTime.ofInstant(inst, ZoneOffset.UTC);
                        if (Timestamp.class.equals(target)) return Timestamp.valueOf(ldt);
                        return ldt;
                    } catch (DateTimeParseException ex) {
                        LocalDateTime ldt = LocalDateTime.parse(s);
                        if (Timestamp.class.equals(target)) return Timestamp.valueOf(ldt);
                        return ldt;
                    }
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("Cannot coerce to LocalDateTime/Timestamp: " + s, ex);
            }
        }

        // Strings: fallback
        if (String.class.equals(target) || CharSequence.class.isAssignableFrom(target)) {
            return s;
        }

        // If target is Object or unknown, return string
        return s;
    }

    /**
     * Helper to render SQL for a hypothetical query — useful in unit tests.
     */
    public String renderSql(List<String> dimensions,
                            List<ValidatedMeasure> measures,
                            List<FilterDto> filters,
                            LocalDateTime from,
                            LocalDateTime to,
                            List<SortDto> sorts,
                            Integer limit,
                            Integer offset,
                            Set<String> allowedTeamIds) {

        final var q = buildSelect(dimensions, measures, filters, from, to, sorts, limit, offset, allowedTeamIds);
        return q.getSQL(ParamType.INLINED);
    }
}
