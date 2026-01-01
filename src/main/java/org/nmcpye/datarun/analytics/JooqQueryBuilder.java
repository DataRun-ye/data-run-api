package org.nmcpye.datarun.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.analytics.dto.QueryFilter;
import org.nmcpye.datarun.analytics.dto.QuerySort;
import org.nmcpye.datarun.analytics.dto.QueryableElementMapping;
import org.nmcpye.datarun.analytics.fieldresolver.AnalyticsFieldResolver;
import org.nmcpye.datarun.jooq.analytics.tables.TallCanonical;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.jooq.analytics.Tables.TALL_CANONICAL;

/**
 * JooqQueryBuilder builds jOOQ Select queries against the UID-native
 * materialized view `pivot_grid_facts`.
 * <p>
 * Responsibilities:
 * - Build typed SELECT statements (without executing) for pivot requests.
 * - Provide a counting method to compute number of result groups.
 * - Ensure deterministic ordering by applying a group-aware tie-breaker
 * (e.g., MIN(value_id) or another stable expression) when ordering is ambiguous.
 * - Translate client supplied filter DTOs into typed jOOQ Conditions using field types
 * from the generated Tables.PIVOT_GRID_FACTS.
 * - Expose helper methods to build aggregate expressions that apply per-measure scoping
 * via `filterWhere(scopeCondition)` when appropriate.
 * <p>
 * Implementation notes:
 * - All predicates and grouping should default to only consider rows where deleted_at IS NULL.
 * - Template-mode measures must scope to te_uid, while global measures use de_uid.
 * - Methods should avoid fluent-type generics fights by using SelectQuery/Select<Record> where helpful.
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JooqQueryBuilder {

    private final DSLContext dsl;
    private static final TallCanonical PG = TALL_CANONICAL;
    private final AnalyticsFieldResolver fieldResolver;

//    private final QueryTarget target;
//    private final QueryableElementMappingFactory mappingFactory;

//    public JooqQueryBuilder(DSLContext dsl, AnalyticsFieldResolver fieldResolver) {
//        this.dsl = dsl;
//        this.fieldResolver = fieldResolver;
//        this.target = new CodegenQueryTarget(PIVOT_GRID_FACTS, "PIVOT_GRID_FACTS");
//        this.mappingFactory = new PivotGridFactsMappingFactory(new CodegenQueryTarget(PIVOT_GRID_FACTS, "PIVOT_GRID_FACTS"));
//    }


    /**
     * Build the jOOQ Select query (without fetching).
     * Ensures deterministic ordering by adding a group-aware tie-breaker (e.g. MIN(value_id))
     * when grouping is present and the requested sorts would otherwise be non-deterministic.
     *
     * @param dimensions     list of MV column names to group by (e.g. "team_uid", "child_category_uid").
     * @param measures       validated and typed measures (ValidatedMeasure) to project/aggregate.
     * @param filters        list of filter DTOs (field/op/value); fields may be aliases or MV columns.
     * @param from           optional inclusive lower bound for submission_completed_at.
     * @param to             optional inclusive upper bound for submission_completed_at.
     * @param sorts          list of sort DTOs (can reference measure aliases or MV columns).
     * @param limit          page size (if null use default).
     * @param offset         page offset (if null default 0).
     * @param allowedTeamIds optional ACL filter applied to team_uid.
     * @return a jOOQ {@link Select<Record>} representing the query (not executed).
     */
    public SelectQuery<Record> buildSelect(
        List<String> dimensions,
        List<QueryableElementMapping> measures,
        List<QueryFilter> filters,
        LocalDateTime from,
        LocalDateTime to,
        List<QuerySort> sorts,
        Integer limit,
        Integer offset,
        Set<String> allowedTeamIds
    ) {
        List<Field<?>> select = new ArrayList<>();
        List<Field<?>> groupBy = new ArrayList<>();
        Map<String, Field<?>> aliasToSelectField = new HashMap<>();

        // dimensions -> groupBy/select
        if (dimensions != null) {
            for (String dimId : dimensions) {
                Field<?> f = fieldResolver.resolveDimensionField(dimId);
                select.add(f);
                groupBy.add(f);
            }
        }

        // measures -> aggregates
        if (measures != null) {
            for (QueryableElementMapping vm : measures) {
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
        Condition cond = PG.IS_DELETED.isNull();
        if (from != null) cond = cond.and(PG.START_TIME.ge(from));
        if (to != null) cond = cond.and(PG.START_TIME.le(to));
        if (allowedTeamIds != null && !allowedTeamIds.isEmpty()) cond = cond.and(PG.TEAM_UID.in(allowedTeamIds));

        if (filters != null) {
            for (var fil : filters) {
                // Resolve the field. If it's a measure alias, use the existing field.
                // Otherwise, resolve it as a dimension field.
                Field<?> f = aliasToSelectField.getOrDefault(fil.field(),
                    fieldResolver.resolveDimensionField(fil.field()));
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
                Field<?> candidate = aliasToSelectField.getOrDefault(s.fieldOrAlias(),
                    fieldResolver.resolveDimensionField(s.fieldOrAlias()));
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
            SortField<?> tie = DSL.min(PG.TALL_ID).asc();
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
     * <p>
     * Semantics:
     * - If dimensions is empty or null, this should return 1 when there are matching rows
     * (after filters and deleted_at IS NULL), otherwise 0.
     * - If dimensions present, returns count of distinct groups (matching group-by projection).
     * <p>
     * Implementation hint:
     * - Prefer generating a COUNT(*) over a derived GROUP BY to avoid huge memory usage.
     *
     * @return number of groups
     */
    public long countGroups(
        List<String> dimensions,
        List<QueryableElementMapping> measures,
        List<QueryFilter> filters,
        LocalDateTime from,
        LocalDateTime to,
        Set<String> allowedTeamIds
    ) {
        // Build the same base condition
        Condition cond = PG.IS_DELETED.isNull();
        if (from != null) cond = cond.and(PG.START_TIME.ge(from));
        if (to != null) cond = cond.and(PG.START_TIME.le(to));
        if (allowedTeamIds != null && !allowedTeamIds.isEmpty()) cond = cond.and(PG.TEAM_UID.in(allowedTeamIds));

        // translate filters (no alias map necessary here)
        if (filters != null) {
            for (var fil : filters) {
                // Note: Filters in countGroups cannot be on measure aliases.
                Field<?> f = fieldResolver.resolveDimensionField(fil.field());
                cond = cond.and(translateFilter(f, fil.op(), fil.value()));
            }
        }

        // group fields
        List<Field<?>> groupBy = new ArrayList<>();
        if (dimensions != null) {
            for (String dimId : dimensions) {
                groupBy.add(fieldResolver.resolveDimensionField(dimId));
            }
        }

        if (groupBy.isEmpty()) {
            // Global aggregate => 1 group if any matching row, else 0
            Integer exists = dsl.select(DSL.one())
                .from(PG)
                .where(cond).limit(1)
                .fetchOne(0, Integer.class);
            return (exists != null) ? 1L : 0L;
        } else {
            // Build a grouped subSelect and COUNT(*) over it
            Select<Record> grouped = dsl.select(groupBy.toArray(new Field<?>[0]))
                .from(PG)
                .where(cond)
                .groupBy(groupBy.toArray(new Field<?>[0]));

            Table<?> sub = grouped.asTable("g");
            Long cnt = dsl.selectCount().from(sub).fetchOne(0, Long.class);
            return cnt == null ? 0L : cnt;
        }
    }


    /**
     * Execute the built query and fetch results.
     * <p>
     * Convenience wrapper: typically delegates to
     * {@link #buildSelect(List, List, List, LocalDateTime, LocalDateTime,
     * List, Integer, Integer, Set) buildSelect(...)}
     * and executes fetch().
     *
     * @return jOOQ {@link Result<Record>} the fetched rows
     * @see #buildSelect(List, List, List,
     * LocalDateTime, LocalDateTime, List, Integer, Integer, Set) buildSelect(...)
     */
    public Result<Record> execute(
        List<String> dimensions,
        List<QueryableElementMapping> measures,
        List<QueryFilter> filters,
        LocalDateTime from,
        LocalDateTime to,
        List<QuerySort> sorts,
        Integer limit,
        Integer offset,
        Set<String> allowedTeamIds
    ) {
        var query = buildSelect(dimensions, measures, filters, from, to, sorts, limit, offset, allowedTeamIds);
        return query.fetch();
    }

    /**
     * Build a typed aggregate field for the given validated measure.
     * <p>
     * Responsibilities:
     * - Use vm.targetField() and vm.aggregation() to produce a correct jOOQ aggregate (SUM, AVG, MIN, MAX, COUNT, COUNT_DISTINCT, SUM_TRUE).
     * - Apply vm.elementPredicate() as a per-aggregate filter using {@code .filterWhere(scope)} semantics, so each measure is scoped to the requested element/template/option.
     * - Assign alias using vm.alias().
     *
     * @param vm validated measure descriptor (typed)
     * @return SelectField<?> representing the aggregate expression aliased.
     */
    @SuppressWarnings("unchecked")
    private Field<?> buildAggregateField(QueryableElementMapping vm) {
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
     * Translate a filter into a typed jOOQ Condition using the supplied field's data type.
     * <p>
     * Rules & features:
     * - Support operators: =, !=, IN, >, <, >=, <=, LIKE, ILIKE
     * - IN with empty collection => policy: match nothing (falseCondition) or throw; implementation must be consistent.
     * - NULL values must map to IS NULL / IS NOT NULL for appropriate operators.
     * - Use DSL.val(coercedValue, dataType) when binding to ensure proper JDBC typing.
     *
     * @param f     typed Field<?> that will be used on the left-hand side of the predicate
     * @param op    operator string (case-insensitive)
     * @param value RHS value (or collection for IN)
     * @return typed Condition suitable to be added to where-clause
     * @throws IllegalArgumentException for unsupported ops or failed coercions
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
                .map(v -> {
                    try {
                        return coerceToType(v, targetClass);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Cannot coerce IN value to " + targetClass.getSimpleName() + ": " + v, ex);
                    }
                })
                .collect(Collectors.toList());

            if (dataType != null) {
                List<Field> vals = coerced.stream()
                    .map(v -> DSL.val(v, (DataType) dataType))
                    .collect(Collectors.toList());
                return ((Field) f).in(vals);
            } else {
                return ((Field) f).in(coerced);
            }
        }

        // NULL handling (no coercion)
        if (value == null) {
            return switch (op.toLowerCase(Locale.ROOT)) {
                case "=", "==", "eq" -> ((Field) f).isNull();
                case "!=", "<>", "neq" -> ((Field) f).isNotNull();
                default -> throw new IllegalArgumentException("Operator " + op + " not supported for NULL value");
            };
        }

        // Operator-specific validation BEFORE coercion for LIKE/ILIKE
        if ("LIKE".equalsIgnoreCase(op) || "ILIKE".equalsIgnoreCase(op)) {
            if (!String.class.equals(targetClass) && !CharSequence.class.isAssignableFrom(targetClass)) {
                throw new IllegalArgumentException(op + " is only valid on string fields");
            }
            // Do not attempt numeric coercion of wildcard patterns — keep as String
            String pattern = value.toString();
            if ("LIKE".equalsIgnoreCase(op)) {
                return ((Field<String>) f).like(pattern);
            } else {
                return ((Field<String>) f).likeIgnoreCase(pattern);
            }
        }

        // Now coerce single value for other operators (safe to attempt)
        final Object coerced;
        try {
            coerced = coerceToType(value, targetClass);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cannot coerce value to " + (targetClass == null ? "unknown" : targetClass.getSimpleName()) + ": " + value, ex);
        }

        Field<?> right = dataType != null ? DSL.val(coerced, (DataType) dataType) : DSL.val(coerced);

        return switch (op.toLowerCase(Locale.ROOT)) {
            case "=", "==", "eq" -> ((Field) f).eq(right);
            case "!=", "<>", "neq" -> ((Field) f).ne(right);
            case ">", "gt" -> ((Field) f).gt(right);
            case "<", "lt" -> ((Field) f).lt(right);
            case ">=", "gte" -> ((Field) f).ge(right);
            case "<=", "lte" -> ((Field) f).le(right);
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    /**
     * Coerce a raw value into the target Java type (based on Field#getDataType().getType()).
     * <p>
     * Conservative conversion rules:
     * - For numeric targets return BigDecimal or narrower primitive wrapper where necessary.
     * - For LocalDateTime/Timestamp accept epoch millis or ISO strings.
     * - For boolean accept "1"/"0", "true"/"false", "yes"/"no".
     * - For unsupported conversions throw IllegalArgumentException.
     *
     * @param raw    input value from client
     * @param target target Java class
     * @return coerced value of type target (or compatible)
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
                            List<QueryableElementMapping> measures,
                            List<QueryFilter> filters,
                            LocalDateTime from,
                            LocalDateTime to,
                            List<QuerySort> sorts,
                            Integer limit,
                            Integer offset,
                            Set<String> allowedTeamIds) {

        final var q = buildSelect(dimensions, measures, filters, from, to, sorts, limit, offset, allowedTeamIds);
        return q.getSQL(ParamType.INLINED);
    }
}
