package org.nmcpye.datarun.jpa.pivot.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * static pivot parameters
 *
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
public interface PivotParameters {
    public enum AggregationType {SUM, COUNT, AVG, MIN, MAX}

    public record DateRange(String from, String to, String groupBy) {
    }

    public record DimensionRef(String dimensionId) {
    }

    public enum FilterOperator {EQUALS, NOT_EQUALS, IN, BETWEEN, GT, LT, GTE, LTE}

    public record FilterRef(String id, FilterOperator operator, List<String> values) {
    }

    public record MeasureRef(String id, AggregationType aggregation, String elementId) {
    }

    public static record Metadata(
        long totalRows,
        int totalPages,
        int currentPage,
        List<String> columnHeaders,
        List<String> measureHeaders,
        Map<String, Object> extras,
        Instant generatedAt,
        String queryPlan // for dev only
    ) {
    }

    public record Pagination(int page, int pageSize) {
    }

    public enum ResultFormat {MATRIX, FLAT}

    public enum SortDirection {ASC, DESC}

    public record Sorting(String id, SortDirection direction) {
    }

    public record Row(
        String dimension,
        List<ColumnValue> values
    ) {}

    public record ColumnValue(
        String column,
        List<BigDecimal> measures
    ) {}
}
