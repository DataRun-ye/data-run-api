//package org.nmcpye.datarun.jpa.pivot.model;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.io.Serial;
//import java.io.Serializable;
//import java.util.List;
//
//import static org.nmcpye.datarun.jpa.pivot.model.PivotParameters.Pagination;
//
///**
// * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
// */
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class PivotGridRequest implements Serializable {
//    @Serial
//    private static final long serialVersionUID = 1L;
//
//    /**
//     * List of columns to group the results by. These should correspond to columns in pivot_grid_facts.
//     * Examples: "team_id", "element_name", "submission_completed_at" (for grouping by date part).
//     */
//    private List<String> dimensions;
//
//    /**
//     * List of measures to aggregate. Each measure specifies the column and the aggregation function.
//     */
//    private List<MeasureDefinition> measures;
//
//    /**
//     * List of filters to apply to the data before aggregation.
//     */
//    private List<FilterDefinition> filters;
//
//    /**
//     * List of sorting criteria for the results.
//     */
//    private List<SortDefinition> sorts;
//
//    /**
//     * Pagination parameters.
//     */
//    private Pagination pagination;
//}
