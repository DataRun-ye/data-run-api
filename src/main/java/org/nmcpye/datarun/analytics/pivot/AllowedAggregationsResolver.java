package org.nmcpye.datarun.analytics.pivot;

import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves allowed aggregations for a ValueType.
 * Exposes strings for use in DTOs so UI can consume them easily.
 */
@Component
public class AllowedAggregationsResolver {
    // Simple mapping of ValueType → allowed aggregations
    private static final Set<Aggregation> DEFAULT_AGGREGATIONS = Set.of(Aggregation.COUNT);

    private static final Set<Aggregation> NUMERIC_AGGREGATIONS = Set.of(
        Aggregation.SUM,
        Aggregation.AVG,
        Aggregation.MIN,
        Aggregation.MAX,
        Aggregation.COUNT,
        Aggregation.COUNT_DISTINCT
    );

    private static final Set<Aggregation> BOOLEAN_AGGREGATIONS = Set.of(
        Aggregation.SUM_TRUE,
        Aggregation.COUNT,
        Aggregation.COUNT_DISTINCT
    );

    private static final Set<Aggregation> TEXT_AGGREGATIONS = Set.of(
        Aggregation.COUNT,
        Aggregation.COUNT_DISTINCT
    );

    private static final Set<Aggregation> DATE_AGGREGATIONS = Set.of(
        Aggregation.MIN,
        Aggregation.MAX,
        Aggregation.COUNT,
        Aggregation.COUNT_DISTINCT
    );

    private static final Set<Aggregation> OPTION_AGGREGATIONS = Set.of(
        Aggregation.COUNT,
        Aggregation.COUNT_DISTINCT
    );

    public Set<String> allowedFor(ValueType vt) {
        if (vt == null) return DEFAULT_AGGREGATIONS.stream()
            .map(Enum::name).collect(Collectors.toSet());
        return switch (vt) {
            case Number, Integer, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive,
                 Percentage, UnitInterval -> NUMERIC_AGGREGATIONS.stream()
                .map(Enum::name).collect(Collectors.toSet());
            case Boolean, TrueOnly -> BOOLEAN_AGGREGATIONS.stream()
                .map(Enum::name).collect(Collectors.toSet());
            case SelectOne, SelectMulti -> OPTION_AGGREGATIONS.stream()
                .map(Enum::name).collect(Collectors.toSet());
            case Text -> TEXT_AGGREGATIONS.stream()
                .map(Enum::name).collect(Collectors.toSet());
            case Date, DateTime, Time -> DATE_AGGREGATIONS.stream()
                .map(Enum::name).collect(Collectors.toSet());
            default -> DEFAULT_AGGREGATIONS.stream()
                .map(Enum::name).collect(Collectors.toSet());
        };
    }
}
