package org.nmcpye.datarun.analytics.pivot;

import org.nmcpye.datarun.analytics.pivot.dto.Aggregation;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

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

    public Set<Aggregation> allowedFor(ValueType vt) {
        if (vt == null) return new HashSet<>(DEFAULT_AGGREGATIONS);
        return switch (vt) {
            case Number, Integer, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive,
                 Percentage, UnitInterval -> new HashSet<>(NUMERIC_AGGREGATIONS);
            case Boolean, TrueOnly -> new HashSet<>(BOOLEAN_AGGREGATIONS);
            case SelectOne, SelectMulti -> new HashSet<>(OPTION_AGGREGATIONS);
            case Text -> new HashSet<>(TEXT_AGGREGATIONS);
            case Date, DateTime, Time -> new HashSet<>(DATE_AGGREGATIONS);
            default -> new HashSet<>(DEFAULT_AGGREGATIONS);
        };
    }
}
