package org.nmcpye.datarun.jpa.pivotdata.dto;


import org.nmcpye.datarun.jpa.pivotdata.model.MeasureDefinition;

import java.util.List;

/**
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public class Measure {
    public final String id;
    public final String displayName;
    public final String sqlExpression;
    public final List<String> supportedAggregations;

    public Measure(String id, String displayName, String sqlExpression, List<String> supportedAggregations) {
        this.id = id;
        this.displayName = displayName;
        this.sqlExpression = sqlExpression;
        this.supportedAggregations = supportedAggregations;
    }

    public static Measure from(MeasureDefinition md) {
        return new Measure(md.getId(), md.getDisplayName(), md.getSqlExpression(),
            md.getSupportedAggregations().stream().map(Enum::name).toList());
    }
}
