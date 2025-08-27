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
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Tests for the private conversion/filter helpers in PivotQueryBuilder.
// * These use reflection to keep the helpers private in production code.
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 24/08/2025
// */
//@SpringBootTest
//class PivotQueryBuilderPrivateMethodsTest {
//
//    @Autowired
//    private PivotQueryBuilder builder;
//
//    private final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;
//
//    private Method convertMethod;
//    private Method filterMethod;
//
//    @BeforeEach
//    void setUp() throws NoSuchMethodException {
//        convertMethod = PivotQueryBuilder.class.getDeclaredMethod("convertSingleValue", Object.class, Class.class);
//        convertMethod.setAccessible(true);
//        filterMethod = PivotQueryBuilder.class.getDeclaredMethod("translateFilterTyped", org.jooq.Field.class, String.class, Object.class);
//        filterMethod.setAccessible(true);
//    }
//
//    @Test
//    void convertStringToBigDecimal() throws InvocationTargetException, IllegalAccessException {
//        Object converted = convertMethod.invoke(builder, "12.34", BigDecimal.class);
//        assertNotNull(converted);
//        assertInstanceOf(BigDecimal.class, converted);
//        assertEquals(new BigDecimal("12.34"), converted);
//    }
//
//    @Test
//    void convertStringToLocalDateTime() throws InvocationTargetException, IllegalAccessException {
//        Object converted = convertMethod.invoke(builder, "2025-08-24T12:34:56", LocalDateTime.class);
//        assertNotNull(converted);
//        assertInstanceOf(LocalDateTime.class, converted);
//        assertEquals(LocalDateTime.parse("2025-08-24T12:34:56"), converted);
//    }
//
//    @Test
//    void translateEqualsNumericCondition() throws InvocationTargetException, IllegalAccessException {
//        // should produce a non-null Condition for numeric equality
//        Object cond = filterMethod.invoke(builder, PG.VALUE_NUM, "=", "42.5");
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//
//    @Test
//    void translateInStringCollectionCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = filterMethod.invoke(builder, PG.TEAM_ID, "IN", List.of("teamA", "teamB"));
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//
//    @Test
//    void translateDateComparisonCondition() throws InvocationTargetException, IllegalAccessException {
//        Object cond = filterMethod.invoke(builder, PG.SUBMISSION_COMPLETED_AT, ">=", "2025-08-01T00:00:00");
//        assertNotNull(cond);
//        assertInstanceOf(Condition.class, cond);
//    }
//}
