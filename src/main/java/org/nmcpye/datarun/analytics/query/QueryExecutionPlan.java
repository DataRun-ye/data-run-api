package org.nmcpye.datarun.analytics.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.nmcpye.datarun.analytics.query.dto.ExecutionHints;

/**
 * @author Hamza Assada
 * @since 13/09/2025
 */
@Data
@AllArgsConstructor
public class QueryExecutionPlan {
    private final SelectQuery<Record> select;
    private final ExecutionHints hints;
}








/*
 * -----------------------------------------------------------------------------
 * Suggested small migration edits for your existing classes
 * -----------------------------------------------------------------------------
 * 1) In JooqQueryBuilder: add a tiny method that produces QueryExecutionPlan
 *    public QueryExecutionPlan buildExecutionPlan(... same args as buildSelect) {
 *        Select<Record> sel = buildSelect(...);
 *        ExecutionHints hints = ExecutionHints.builder()
 *                             .canPushMatrixToDB(false) // or compute heuristics here
 *                             .preferMaterializedView(false).build();
 *        return new QueryExecutionPlan(sel, hints);
 *    }
 *
 * 2) Add a new component bean in your Spring config:
 *    @Bean
 *    public QueryExecutor queryExecutor(DSLContext dsl, PlatformTransactionManager txManager) {
 *        return new JooqQueryExecutor(dsl, new TransactionTemplate(txManager));
 *    }
 *
 * 3) Modify QueryServiceImpl.query(...) execution path: instead of always doing dsl.fetch(select)
 *    ask the builder for a QueryExecutionPlan and then use queryExecutor.executeStream(plan, record -> { ... })
 *    to either directly stream output to the HTTP response (NDJSON) or collect incremental results in a memory-conscious way.
 *
 * The code below is intentionally compact and focused on the new pieces. Apply incrementally — keep the original methods
 * and add these as new helpers so you can swap over calls one endpoint at a time.
 */

// End of patch file
