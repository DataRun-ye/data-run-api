//package org.nmcpye.datarun.jpa.pivot;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
///**
// * Request describing a measure (element + aggregation).
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class MeasureRequest {
//    /**
//     * Element identifier (element_id in element_template_config).
//     */
//    private String elementId;
//
//    /**
//     * Aggregation requested (SUM, AVG, COUNT, COUNT_DISTINCT, MIN, MAX, SUM_TRUE).
//     */
//    private Aggregation aggregation;
//
//    /**
//     * Optional alias to use in the result.
//     */
//    private String alias;
//
//    /**
//     * If true and aggregation is COUNT, use COUNT(DISTINCT ...).
//     */
//    private Boolean distinct;
//}
