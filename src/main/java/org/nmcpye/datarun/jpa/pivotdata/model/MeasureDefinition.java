package org.nmcpye.datarun.jpa.pivotdata.model;

import lombok.Getter;

import java.util.List;

/**
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
@Getter
public class MeasureDefinition {
    public enum Aggregation {SUM, COUNT, AVG, MIN, MAX}

    private final String id;             // e.g. "element.Xxx" or "submission.count"
    private final String displayName;
    private final String sqlExpression;  // column or expression to aggregate (e.g. value_num)
    private final List<Aggregation> supportedAggregations;

    public MeasureDefinition(String id, String displayName, String sqlExpression, List<Aggregation> supportedAggregations) {
        this.id = id;
        this.displayName = displayName;
        this.sqlExpression = sqlExpression;
        this.supportedAggregations = supportedAggregations;
    }
}
