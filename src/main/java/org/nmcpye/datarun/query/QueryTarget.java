package org.nmcpye.datarun.query;

import org.jooq.Record;
import org.jooq.Table;

/**
 * QueryTarget - tiny abstraction for a DB target (table / MV)
 *
 * @author Hamza Assada
 * @since 14/09/2025
 */
public interface QueryTarget {
    /**
     * jOOQ table reference (e.g. codegen class like PIVOT_GRID_FACTS)
     */
    Table<? extends Record> table();

    /**
     * Friendly id used in logs/telemetry (e.g. "PIVOT_GRID_FACTS-v1").
     */
    String id();

    /**
     * Optional helper to quickly assert support for columns/dimensions. Keep simple.
     */
    default boolean supportsDimension(String dimensionName) {
        return true;
    }
}


// -----------------------------------------------------------------------------
// 4)
// -----------------------------------------------------------------------------


// -----------------------------------------------------------------------------
// 5) Suggested JooqQueryBuilder changes (PATCH notes - do not paste in codebase unchanged)
// -----------------------------------------------------------------------------
/*
 In JooqQueryBuilder:
  - Add constructors that accept: QueryTarget target, QueryableElementMappingFactory mappingFactory
  - Keep an existing no-arg/default constructor that delegates to the existing behavior by creating
    a CodegenQueryTarget(PIVOT_GRID_FACTS, "PIVOT_GRID_FACTS") and a PivotGridFactsMappingFactory.
  - Replace any direct references to PIVOT_GRID_FACTS in the builder with target.table().field("<col>")
    or, better, keep type-safe Field access by using the mappingFactory to produce the targetField.

 Example changes (conceptual):
 public class JooqQueryBuilder {
     private final DSLContext ctx;
     private final QueryTarget target;
     private final QueryableElementMappingFactory mappingFactory;

     // existing constructor kept for convenience/backwards compatibility
     public JooqQueryBuilder(DSLContext ctx) {
         this(ctx, new CodegenQueryTarget(PIVOT_GRID_FACTS, "PIVOT_GRID_FACTS"),
              new PivotGridFactsMappingFactory(new CodegenQueryTarget(PIVOT_GRID_FACTS, "PIVOT_GRID_FACTS")));
     }

     public JooqQueryBuilder(DSLContext ctx, QueryTarget target, QueryableElementMappingFactory factory) {
         this.ctx = ctx;
         this.target = target;
         this.mappingFactory = factory;
     }

     // translation now uses mappingFactory.build(measure, resolver) to obtain QueryableElementMapping
 }
*/

// -----------------------------------------------------------------------------
// 6) Suggested MeasureValidationServiceImpl change (PATCH notes)
// -----------------------------------------------------------------------------
/*
 In MeasureValidationServiceImpl:
  - Remove the direct mapping-to-Field code path and instead delegate to a QueryableElementMappingFactory
    (injected into the service, or provided by a small helper)
  - Keep existing validation semantics; factory focuses on mapping only (but may re-check types)
  - Unit-tests should assert that existing alias names and produced QueryableElementMapping objects match
    previous outputs. Use DSLContext.renderInlined(...) to compare generated expressions where helpful.
*/

// -----------------------------------------------------------------------------
// 7) Unit test template (java) - create a new test class that asserts SQL rendering parity
// -----------------------------------------------------------------------------

/*
import org.junit.jupiter.api.Test;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

public class QueryBuilderSqlRenderingTest {
    @Test
    public void baselineSql_shouldRemainSame_afterRefactor() {
        DSLContext ctx = DSL.using(SQLDialect.POSTGRES);
        // 1) build query using existing legacy builder (oldBuilder)
        String oldSql = ctx.renderInlined(oldBuilder.build(...));

        // 2) build query using refactored builder wired with CodegenQueryTarget + PivotGridFactsMappingFactory
        String newSql = ctx.renderInlined(newBuilder.build(...));

        assertEquals(oldSql, newSql);
    }
}
*/

// -----------------------------------------------------------------------------
// End of file. Apply the above with small commits; run baseline tests after Step 1 and Step 2.
// -----------------------------------------------------------------------------
