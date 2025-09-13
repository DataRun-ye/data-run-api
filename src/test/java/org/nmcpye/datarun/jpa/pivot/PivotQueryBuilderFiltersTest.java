//package org.nmcpye.datarun.jpa.pivot;
//
//import org.jooq.Condition;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.nmcpye.datarun.jooq.Tables;
//import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertInstanceOf;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
///**
// * Tests for enriched filter operators in JooqQueryBuilder.
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@SpringBootTest
//class
//JooqQueryBuilderFiltersTest {
//
//    @Autowired
//    private JooqQueryBuilder builder;
//
//    private final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;
//
//    private Method translateFilterMethod;
//
//    @BeforeEach
//    void setUp() throws NoSuchMethodException {
//        translateFilterMethod = JooqQueryBuilder.class.getDeclaredMethod("translateFilterTyped", org.jooq.Field.class, String.class, Object.class);
//        translateFilterMethod.setAccessible(true);
//    }
//
//    @Test
//    void likeOnTextFieldProducesCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = translateFilterMethod.invoke(builder, PG.VALUE_TEXT, "LIKE", "abc%");
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//
//    @Test
//    void ilikeOnTextFieldProducesCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = translateFilterMethod.invoke(builder, PG.VALUE_TEXT, "ILIKE", "%abc%");
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//
//    @Test
//    void betweenOnNumericFieldProducesCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = translateFilterMethod.invoke(builder, PG.VALUE_NUM, "BETWEEN", List.of("1.0", "10.5"));
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//
//    @Test
//    void isDistinctFromProducesCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = translateFilterMethod.invoke(builder, PG.CATEGORY_ID, "IS DISTINCT FROM", "cat-1");
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//
//    @Test
//    void isNotDistinctFromProducesCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = translateFilterMethod.invoke(builder, PG.CATEGORY_ID, "IS NOT DISTINCT FROM", "cat-1");
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//
//    @Test
//    void inOperatorWithCollectionProducesCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = translateFilterMethod.invoke(builder, PG.TEAM_ID, "IN", List.of("t1", "t2", "t3"));
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//}
