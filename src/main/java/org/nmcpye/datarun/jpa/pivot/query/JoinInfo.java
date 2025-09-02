//package org.nmcpye.datarun.jpa.pivot.query;
//
//import org.jooq.Condition;
//import org.jooq.Table;
//
//import java.util.Objects;
//
///**
// * Represents a join required to access a field.
// *
// * @param table       the jOOQ Table to join (e.g., ASSIGNMENT)
// * @param onCondition the ON condition (e.g., ELEMENT_DATA_VALUE.ASSIGNMENT_ID.eq(ASSIGNMENT.ID))
// *                    // * @param joinType    the join type (LEFT_OUTER_JOIN or JOIN)
//// * @param alias       optional stable alias for the joined table (if null builder will generate one)
// * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
// * @see PivotableFieldMapping
// * @see PivotRegistry
// */
//public record JoinInfo(Table<?> table, Condition onCondition) {
//    public JoinInfo {
//        Objects.requireNonNull(table, "table");
//        Objects.requireNonNull(onCondition, "onCondition");
////        Objects.requireNonNull(joinType, "joinType");
//    }
//}
