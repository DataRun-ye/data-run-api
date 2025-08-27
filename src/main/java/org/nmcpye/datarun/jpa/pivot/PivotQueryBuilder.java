//package org.nmcpye.datarun.jpa.pivot;
//
//import lombok.RequiredArgsConstructor;
//import org.jooq.Record;
//import org.jooq.*;
//import org.jooq.impl.DSL;
//import org.nmcpye.datarun.jooq.Tables;
//import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeParseException;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Builds and executes pivot (group-by) queries against the pivot_grid_facts materialized view.
// * Uses jOOQ generated tables via Tables.PIVOT_GRID_FACTS.
// * Handles typed filter conversions for jOOQ Field<T>.eq(T) signatures.
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@Component
//@RequiredArgsConstructor
//public class PivotQueryBuilder {
//
//    private final DSLContext dsl;
//    private final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;
//
//    public static record Filter(String field, String op, Object value) {
//    }
//
//    public static record Sort(String field, boolean desc) {
//    }
//
//    public Result<Record> execute(
//            List<String> dimensions,
//            List<ValidatedMeasure> measures,
//            List<Filter> filters,
//            LocalDateTime from,
//            LocalDateTime to,
//            List<Sort> sorts,
//            Integer limit,
//            Integer offset,
//            Set<String> allowedTeamIds
//    ) {
//        List<SelectField<?>> select = new ArrayList<>();
//        List<Field<?>> groupBy = new ArrayList<>();
//
//        if (dimensions != null) {
//            for (String dim : dimensions) {
//                Field<?> f = resolveFactField(dim);
//                select.add(f);
//                groupBy.add(f);
//            }
//        }
//
//        if (measures != null) {
//            for (ValidatedMeasure vm : measures) {
//                Field<?> aggExpr = buildAggregate(vm);
//                select.add(aggExpr.as(vm.getAlias()));
//            }
//        }
//
//        if (select.isEmpty()) {
//            select.add(DSL.count().as("count"));
//        }
//
//        SelectQuery<Record> query = dsl.selectQuery();
//        query.addSelect(select.toArray(new SelectField<?>[0]));
//        query.addFrom(PG);
//
//        Condition cond = PG.DELETED_AT.isNull();
//
//        if (from != null && to != null) {
//            cond = cond.and(PG.SUBMISSION_COMPLETED_AT.between(from, to));
//        } else if (from != null) {
//            cond = cond.and(PG.SUBMISSION_COMPLETED_AT.ge(from));
//        } else if (to != null) {
//            cond = cond.and(PG.SUBMISSION_COMPLETED_AT.le(to));
//        }
//
//        if (allowedTeamIds != null && !allowedTeamIds.isEmpty()) {
//            cond = cond.and(PG.TEAM_ID.in(allowedTeamIds));
//        }
//
//        if (filters != null) {
//            for (Filter fil : filters) {
//                Field<?> f = resolveFactField(fil.field());
//                cond = cond.and(translateFilterTyped(f, fil.op(), fil.value()));
//            }
//        }
//
//        query.addConditions(cond);
//
//        if (!groupBy.isEmpty()) {
//            query.addGroupBy(groupBy.toArray(new Field<?>[0]));
//        }
//
//        if (sorts != null && !sorts.isEmpty()) {
//            List<SortField<?>> sortFields = sorts.stream().map(s -> {
//                Field<?> f = resolveFactFieldOrAlias(s.field());
//                return s.desc() ? f.desc() : f.asc();
//            }).collect(Collectors.toList());
//            query.addOrderBy(sortFields.toArray(new SortField<?>[0]));
//        }
//
//        query.addLimit(limit == null ? 100 : limit);
//        query.addOffset(offset == null ? 0 : offset);
//
//        return query.fetch();
//    }
//
//    @SuppressWarnings({"unchecked", "rawtypes"})
//    private Field<?> buildAggregate(ValidatedMeasure vm) {
//        Field<?> target = vm.getTargetColumn();
//        Condition filter = vm.getElementFilter();
//        String alias = vm.getAlias();
//        boolean distinct = vm.isDistinct();
//
//        switch (vm.getAggregation()) {
//            case SUM -> {
//                // numeric sum
//                // try filtered aggregate first
//                try {
//                    return DSL.sum((Field<BigDecimal>) target).filterWhere(filter).as(alias);
//                } catch (NoSuchMethodError | AbstractMethodError ignored) {
//                    // fallback to case WHEN
//                    return DSL.sum(DSL.when((Condition) filter, (Field<BigDecimal>) target)
//                        .otherwise((BigDecimal) null)).as(alias);
//                }
//            }
//            case AVG -> {
//                try {
//                    return DSL.avg((Field<BigDecimal>) target).filterWhere(filter).as(alias);
//                } catch (Throwable ignored) {
//                    return DSL.avg(DSL.when((Condition) filter, (Field<BigDecimal>) target)
//                        .otherwise((BigDecimal) null)).as(alias);
//                }
//            }
//            case MIN -> {
//                try {
//                    return DSL.min(target).filterWhere(filter).as(alias);
//                } catch (Throwable ignored) {
//                    return DSL.min(DSL.when((Condition) filter, target)
//                        .otherwise(null)).as(alias);
//                }
//            }
//            case MAX -> {
//                try {
//                    return DSL.max(target).filterWhere(filter).as(alias);
//                } catch (Throwable ignored) {
//                    return DSL.max(DSL.when((Condition) filter, target)
//                        .otherwise((Object) null))
//                        .as(alias);
//                }
//            }
//            case COUNT -> {
//                if (distinct) {
//                    try {
//                        return DSL.countDistinct(target).filterWhere(filter).as(alias);
//                    } catch (Throwable ignored) {
//                        return DSL.countDistinct(DSL.when(filter, target)
//                            .otherwise((Object) null)).as(alias);
//                    }
//                } else {
//                    try {
//                        // count(*) filtered
//                        return DSL.count().filterWhere(filter).as(alias);
//                    } catch (Throwable ignored) {
//                        return DSL.sum(DSL.when(filter, DSL.inline(1)).otherwise(0)).as(alias);
//                    }
//                }
//            }
//            case COUNT_DISTINCT -> {
//                try {
//                    return DSL.countDistinct(target).filterWhere(filter).as(alias);
//                } catch (Throwable ignored) {
//                    return DSL.countDistinct(DSL.when(filter, target)
//                        .otherwise((Object) null)).as(alias);
//                }
//            }
//            case SUM_TRUE -> {
//                // boolean true count -> SUM(CASE WHEN value_bool THEN 1 ELSE 0 END) FILTER (...)
//                Field<Integer> caseExpr = DSL.when((Condition) vm.getTargetColumn().equal(true), DSL.inline(1)).otherwise(0);
//                // Note: targetColumn eq true is equivalent to PG.VALUE_BOOL.eq(true) but vm.elementFilter already restricts element_id
//                try {
//                    return DSL.sum(caseExpr).filterWhere(filter).as(alias);
//                } catch (Throwable ignored) {
//                    return DSL.sum(DSL.when(filter.and(((Condition)
//                        vm.getTargetColumn().equal(true))),
//                        DSL.inline(1)).otherwise(0)).as(alias);
//                }
//            }
//            default -> {
//                // safe fallback : count
//                return DSL.count().filterWhere(filter).as(alias);
//            }
//        }
//    }
//
////    @SuppressWarnings("unchecked")
////    private Field<?> buildAggregate(MeasureValidationService.ValidatedMeasure vm) {
////        Field<?> target = resolveFactField(vm.getColumn());
////        switch (vm.getAggregation()) {
////            case SUM -> {
////                return DSL.sum((Field<BigDecimal>) target);
////            }
////            case AVG -> {
////                return DSL.avg((Field<BigDecimal>) target);
////            }
////            case MIN -> {
////                return DSL.min(target);
////            }
////            case MAX -> {
////                return DSL.max(target);
////            }
////            case COUNT -> {
////                return vm.isDistinct() ? DSL.countDistinct((Field<?>) target) : DSL.count();
////            }
////            case COUNT_DISTINCT -> {
////                return DSL.countDistinct((Field<?>) target);
////            }
////            case SUM_TRUE -> {
////                Field<Integer> caseExpr = DSL.when(PG.VALUE_BOOL.eq(true), DSL.inline(1)).otherwise(0);
////                return DSL.sum(caseExpr);
////            }
////            default -> {
////                return DSL.count();
////            }
////        }
////    }
////
//    private Field<?> resolveFactField(String column) {
//        if (column == null) return DSL.field("null");
//        return switch (column) {
//            case "team_id" -> PG.TEAM_ID;
//            case "org_unit_id" -> PG.ORG_UNIT_ID;
//            case "activity_id" -> PG.ACTIVITY_ID;
//            case "element_id" -> PG.ELEMENT_ID;
//            case "option_id" -> PG.OPTION_ID;
//            case "value_num" -> PG.VALUE_NUM;
//            case "value_text" -> PG.VALUE_TEXT;
//            case "value_bool" -> PG.VALUE_BOOL;
//            case "submission_id" -> PG.SUBMISSION_ID;
//            case "submission_completed_at" -> PG.SUBMISSION_COMPLETED_AT;
//            case "repeat_instance_id" -> PG.REPEAT_INSTANCE_ID;
//            case "category_id" -> PG.CATEGORY_ID;
//            default -> PG.field(column);
//        };
//    }
//
//    private Field<?> resolveFactFieldOrAlias(String nameOrAlias) {
//        try {
//            return resolveFactField(nameOrAlias);
//        } catch (Exception e) {
//            return DSL.field(DSL.name(nameOrAlias));
//        }
//    }
//
//    /**
//     * Build a typed Condition by converting input values to the Field's Java type first.
//     * Supports =, !=, IN, >, <, >=, <=, LIKE, ILIKE, BETWEEN, IS DISTINCT FROM, IS NOT DISTINCT FROM.
//     */
//    @SuppressWarnings({"unchecked", "rawtypes"})
//    private <T> Condition translateFilterTyped(Field<T> field, String op, Object value) {
//        String opNorm = op == null ? "=" : op.strip().toUpperCase(Locale.ROOT);
//        Class<?> targetType = field.getDataType().getType();
//
//        // IS NULL / IS NOT NULL (special-case if op provided as these)
//        if ("IS NULL".equals(opNorm) || ("IS".equals(opNorm) && value == null)) {
//            return field.isNull();
//        }
//        if ("IS NOT NULL".equals(opNorm) || ("IS NOT".equals(opNorm) && value == null)) {
//            return field.isNotNull();
//        }
//
//        // value==null handled for equality/ne
//        if (value == null) {
//            return switch (opNorm) {
//                case "=" -> field.isNull();
//                case "!=" -> field.isNotNull();
//                default ->
//                        throw new IllegalArgumentException("Null value only supports '=' or '!=' or IS NULL operators");
//            };
//        }
//
//        // IN operator with collection
//        if ("IN".equals(opNorm)) {
//            if (!(value instanceof Collection<?> coll)) {
//                throw new IllegalArgumentException("IN operator requires a collection value");
//            }
//            Collection<?> converted = coll.stream()
//                    .map(v -> convertSingleValue(v, targetType))
//                    .collect(Collectors.toList());
//            return field.in((Collection<T>) converted);
//        }
//
//        // BETWEEN: expect collection/array/2-length list or two-value array
//        if ("BETWEEN".equals(opNorm)) {
//            Object lowHigh = value;
//            Object lowObj = null;
//            Object highObj = null;
//            if (lowHigh instanceof Collection<?> coll) {
//                if (coll.size() != 2) throw new IllegalArgumentException("BETWEEN requires exactly two values");
//                Iterator<?> it = coll.iterator();
//                lowObj = it.next();
//                highObj = it.next();
//            } else if (lowHigh.getClass().isArray()) {
//                Object[] arr = (Object[]) lowHigh;
//                if (arr.length != 2) throw new IllegalArgumentException("BETWEEN requires exactly two values");
//                lowObj = arr[0];
//                highObj = arr[1];
//            } else {
//                throw new IllegalArgumentException("BETWEEN requires a collection/array of two elements");
//            }
//            Object lowConv = convertSingleValue(lowObj, targetType);
//            Object highConv = convertSingleValue(highObj, targetType);
//            return ((Field) field).between((T) lowConv, (T) highConv);
//        }
//
//        // LIKE / ILIKE (string comparison). We'll cast field to String if necessary.
//        if ("LIKE".equals(opNorm) || "ILIKE".equals(opNorm)) {
//            String pattern = value.toString();
//            // If field is string typed, use it; otherwise cast to VARCHAR and apply like
//            if (String.class.equals(targetType)) {
//                Field<String> sf = (Field<String>) field;
//                return "LIKE".equals(opNorm) ? sf.like(pattern) : sf.likeIgnoreCase(pattern);
//            } else {
//                Field<String> casted = ((Field) field).cast(String.class);
//                return "LIKE".equals(opNorm) ? casted.like(pattern) : casted.likeIgnoreCase(pattern);
//            }
//        }
//
//        // IS DISTINCT FROM / IS NOT DISTINCT FROM
//        if ("IS DISTINCT FROM".equals(opNorm)) {
//            Object conv = convertSingleValue(value, targetType);
//            return ((Field) field).isDistinctFrom(conv);
//        }
//        if ("IS NOT DISTINCT FROM".equals(opNorm)) {
//            Object conv = convertSingleValue(value, targetType);
//            return ((Field) field).isNotDistinctFrom(conv);
//        }
//
//        // Fallback: single-value comparison ops (=, !=, >, <, >=, <=)
//        Object converted = convertSingleValue(value, targetType);
//        T typed = (T) converted;
//
//        return switch (opNorm) {
//            case "=" -> field.eq(typed);
//            case "!=" -> field.ne(typed);
//            case ">" -> ((Field) field).gt(typed);
//            case "<" -> ((Field) field).lt(typed);
//            case ">=" -> ((Field) field).ge(typed);
//            case "<=" -> ((Field) field).le(typed);
//            default -> throw new IllegalArgumentException("Unsupported filter op: " + op);
//        };
//    }
//
//    /**
//     * Convert a single object to the Field's Java type (common conversions).
//     */
//    private Object convertSingleValue(Object value, Class<?> targetType) {
//        if (value == null) return null;
//
//        if (targetType.isInstance(value)) {
//            return value;
//        }
//
//        // Strings -> typed conversions
//        if (value instanceof String s) {
//            if (targetType == BigDecimal.class) {
//                return new BigDecimal(s);
//            } else if (targetType == Integer.class || targetType == int.class) {
//                return Integer.valueOf(s);
//            } else if (targetType == Long.class || targetType == long.class) {
//                return Long.valueOf(s);
//            } else if (targetType == Double.class || targetType == double.class) {
//                return Double.valueOf(s);
//            } else if (targetType == Boolean.class || targetType == boolean.class) {
//                return Boolean.valueOf(s);
//            } else if (targetType == LocalDateTime.class) {
//                try {
//                    return LocalDateTime.parse(s);
//                } catch (DateTimeParseException ex) {
//                    try {
//                        long epoch = Long.parseLong(s);
//                        return LocalDateTime.ofEpochSecond(epoch / 1000L, 0, java.time.ZoneOffset.UTC);
//                    } catch (Exception ignore) {
//                    }
//                    throw new IllegalArgumentException("Cannot parse LocalDateTime from: " + s);
//                }
//            } else {
//                return s;
//            }
//        }
//
//        // Numbers
//        if (value instanceof Number n) {
//            if (targetType == BigDecimal.class) return BigDecimal.valueOf(n.doubleValue());
//            if (targetType == Integer.class || targetType == int.class) return n.intValue();
//            if (targetType == Long.class || targetType == long.class) return n.longValue();
//            if (targetType == Double.class || targetType == double.class) return n.doubleValue();
//            return n;
//        }
//
//        if (value instanceof Boolean b && (targetType == Boolean.class || targetType == boolean.class)) {
//            return b;
//        }
//
//        // Last resort, return as-is
//        return value;
//    }
//}
