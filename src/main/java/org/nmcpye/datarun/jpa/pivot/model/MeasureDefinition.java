//package org.nmcpye.datarun.jpa.pivot.model;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.io.Serial;
//import java.io.Serializable;
//
///**
// * @author Hamza Assada 23/08/2025 (7amza.it@gmail.com)
// */
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class MeasureDefinition implements Serializable {
//    @Serial
//    private static final long serialVersionUID = 1L;
//
//    public enum AggregationFunction {
//        SUM, COUNT, AVG, MIN, MAX, COUNT_DISTINCT
//    }
//
//    /**
//     * The column from pivot_grid_facts to aggregate.
//     * Examples: "value_num", "value_id" (for count).
//     */
//    private String column;
//
//    /**
//     * The aggregation function to apply.
//     */
//    private AggregationFunction function;
//
//    /**
//     * An alias for the aggregated measure in the response. If null, a default will be used (e.g., "SUM_value_num").
//     */
//    private String alias;
//}
//
//// --- Response DTOs ---
