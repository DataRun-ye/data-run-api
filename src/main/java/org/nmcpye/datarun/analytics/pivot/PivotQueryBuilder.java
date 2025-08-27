package org.nmcpye.datarun.analytics.pivot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
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
public class PivotQueryBuilder {

    private final DSLContext dsl;

    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;

    // Simple filter DTO
    public static record Filter(String field, String op, Object value) {
    }

    // Sort DTO
    public static record Sort(String fieldOrAlias, boolean desc) {
    }

    /**
     * Build the jOOQ Select query (without fetching). Useful for tests that want to assert SQL.
     * The execute(...) method will call this and fetch results in production.
     */
    public Select<Record> buildSelect(
        List<String> dimensions,
        List<ValidatedMeasure> measures,
        List<Filter> filters,
        LocalDateTime from,
        LocalDateTime to,
        List<Sort> sorts,
        Integer limit,
        Integer offset,
        Set<String> allowedTeamIds
    ) {
        // build selects and groupBys
        List<Field<?>> select = new ArrayList<>();
        List<Field<?>> groupBy = new ArrayList<>();

        // Keep a map alias->SelectField for resolving sorts referencing aliases
        Map<String, Field<?>> aliasToSelectField = new HashMap<>();

        // Dimensions
        if (dimensions != null) {
            for (String dim : dimensions) {
                Field<?> f = resolveFactField(dim);
                select.add(f);
                groupBy.add(f);
            }
        }

        // Measures: build aggregate expressions (select fields)
        if (measures != null) {
            for (ValidatedMeasure vm : measures) {
                Field<?> agg = buildAggregateField(vm);
                select.add(agg);
                aliasToSelectField.put(vm.alias(), agg);
            }
        }

        // Ensure at least something selected
        if (select.isEmpty()) {
            select.add(DSL.count().as("count"));
        }

        // Build a SelectQuery to allow incremental additions without generic issues
        SelectQuery<Record> query = dsl.selectQuery();
        query.addSelect(select.toArray(new SelectField<?>[0]));
        query.addFrom(PG);

        // Basic condition: non-deleted facts
        Condition cond = PG.DELETED_AT.isNull();

        // Time range
        if (from != null && to != null) {
            cond = cond.between(PG.SUBMISSION_COMPLETED_AT.ge(from),
                PG.SUBMISSION_COMPLETED_AT.le(to));
        } else {
            if (from != null) cond = cond.and(PG.SUBMISSION_COMPLETED_AT.ge(from));
            if (to != null) cond = cond.and(PG.SUBMISSION_COMPLETED_AT.le(to));
        }

        // ACL
        if (allowedTeamIds != null && !allowedTeamIds.isEmpty()) {
            cond = cond.and(PG.TEAM_ID.in(allowedTeamIds));
        }

        // Additional filters
        if (filters != null && !filters.isEmpty()) {
            for (Filter f : filters) {
                Field<?> field = resolveFactFieldOrAlias(f.field(), aliasToSelectField);
                Condition c = translateFilter(field, f.op(), f.value());
                cond = cond.and(c);
            }
        }

        query.addConditions(cond);

        // Group by (if any)
        if (!groupBy.isEmpty()) {
            query.addGroupBy(groupBy.toArray(new Field<?>[0]));
        }

//        // Sorting
//        if (sorts != null && !sorts.isEmpty()) {
//            List<SortField<?>> sortFields = new ArrayList<>();
//            for (Sort s : sorts) {
//                String nameOrAlias = s.fieldOrAlias();
//                Field<?> candidate = aliasToSelectField.get(nameOrAlias);
//                if (candidate == null) {
//                    candidate = resolveFactField(nameOrAlias);
//                }
//                // Now candidate is a Field<?> — call asc()/desc() and collect the SortField
//                SortField<?> sf = s.desc() ? candidate.desc() : candidate.asc();
//                sortFields.add(sf);
//            }
//            query.addOrderBy(sortFields.toArray(new SortField<?>[0]));
//        }
// Sorting (deterministic tie-breaker)
// Build list of SortField<?> and keep track of seen keys (aliases or field names)
        if (sorts != null && !sorts.isEmpty()) {
            List<SortField<?>> sortFields = new ArrayList<>();
            Set<String> seenSortKeys = new HashSet<>();

            for (Sort s : sorts) {
                String nameOrAlias = s.fieldOrAlias();
                Field<?> candidate = aliasToSelectField.get(nameOrAlias);
                if (candidate == null) {
                    candidate = resolveFactField(nameOrAlias);
                }
                String seenKey = nameOrAlias != null ? nameOrAlias : candidate.getName();
                seenSortKeys.add(seenKey);

                SortField<?> sf = s.desc() ? candidate.desc() : candidate.asc();
                sortFields.add(sf);
            }

            // append group-by fields not already present as tie-breakers
            for (Field<?> gb : groupBy) {
                String gbName = gb.getName();
                if (!seenSortKeys.contains(gbName)) {
                    sortFields.add(gb.asc());
                    seenSortKeys.add(gbName);
                }
            }

            // final stable tie-breaker:
            // - If grouped (groupBy not empty): use MIN(value_id) or MAX(value_id) to produce a deterministic single value per group
            // - If not grouped: use the raw VALUE_ID
            if (!seenSortKeys.contains("value_id")) {
                if (!groupBy.isEmpty()) {
                    // MIN(value_id) ASC
                    sortFields.add(DSL.min(PG.VALUE_ID).asc());
                } else {
                    sortFields.add(PG.VALUE_ID.asc());
                }
            }

            query.addOrderBy(sortFields.toArray(new SortField<?>[0]));

        } else {
            // No sorts requested: deterministic default = groupBy fields (asc) then tie-breaker
            List<SortField<?>> defaultSorts = new ArrayList<>();
            for (Field<?> gb : groupBy) {
                defaultSorts.add(gb.asc());
            }
            if (!groupBy.isEmpty()) {
                // grouped => use MIN(value_id) as final tie-breaker
                defaultSorts.add(DSL.min(PG.VALUE_ID).asc());
            } else {
                defaultSorts.add(PG.VALUE_ID.asc());
            }
            query.addOrderBy(defaultSorts.toArray(new SortField<?>[0]));
        }
        // Pagination
        query.addLimit(limit == null ? 100 : limit);
        if (offset != null) query.addOffset(offset);

        // Execute
        return query;
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
        List<Filter> filters,
        LocalDateTime from,
        LocalDateTime to,
        List<Sort> sorts,
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
            case "team_id" -> PG.TEAM_ID;
            case "org_unit_id" -> PG.ORG_UNIT_ID;
            case "activity_id" -> PG.ACTIVITY_ID;
            case "element_id" -> PG.ELEMENT_ID;
            case "option_id" -> PG.OPTION_ID;
            case "value_num" -> PG.VALUE_NUM;
            case "value_text" -> PG.VALUE_TEXT;
            case "value_bool" -> PG.VALUE_BOOL;
            case "value_ts" -> PG.VALUE_TS;
            case "submission_id" -> PG.SUBMISSION_ID;
            case "submission_completed_at" -> PG.SUBMISSION_COMPLETED_AT;
            case "repeat_instance_id" -> PG.REPEAT_INSTANCE_ID;
            case "category_id" -> PG.CHILD_CATEGORY_ID;
            case "element_template_config_id", "element_config_id" -> PG.ELEMENT_CONFIG_ID;
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
                            List<Filter> filters,
                            LocalDateTime from,
                            LocalDateTime to,
                            List<Sort> sorts,
                            Integer limit,
                            Integer offset,
                            Set<String> allowedTeamIds) {

        final var q = buildSelect(dimensions, measures, filters, from, to, sorts, limit, offset, allowedTeamIds);
        return q.getSQL(ParamType.INLINED);
    }
}
