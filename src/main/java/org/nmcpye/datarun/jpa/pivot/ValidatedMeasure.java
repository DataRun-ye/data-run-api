//package org.nmcpye.datarun.jpa.pivot;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.jooq.Condition;
//import org.jooq.Field;
//
///**
// * Validated measure produced by MeasureValidationService and consumed by PivotQueryBuilder.
// *
// * @author Hamza Assada
// * @since 26/08/2025
// */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ValidatedMeasure {
//    private String elementId;
//    private PivotMeasureRequest.Aggregation aggregation;
//    private String alias;
//    private Field<?> targetColumn;
//    private Condition elementFilter;
//    private boolean distinct;
//    private String optionId;
//
//    public String getElementId() {
//        return elementId;
//    }
//
//    public PivotMeasureRequest.Aggregation getAggregation() {
//        return aggregation;
//    }
//
//    public String getAlias() {
//        return alias;
//    }
//
//    public Field<?> getTargetColumn() {
//        return targetColumn;
//    }
//
//    public Condition getElementFilter() {
//        return elementFilter;
//    }
//
//    public boolean isDistinct() {
//        return distinct;
//    }
//
//    public String getOptionId() {
//        return optionId;
//    }
//}
