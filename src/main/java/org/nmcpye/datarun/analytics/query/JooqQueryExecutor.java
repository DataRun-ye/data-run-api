package org.nmcpye.datarun.analytics.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple jOOQ-based executor that uses TransactionTemplate to ensure a transaction (autoCommit=false)
 * so Postgres JDBC can use server-side cursors when fetchSize is set.
 *
 * @author Hamza Assada
 * @since 13/09/2025
 */
@Slf4j
@RequiredArgsConstructor
class JooqQueryExecutor implements QueryExecutor {
    private final DSLContext dsl;
    private final TransactionTemplate txTemplate;
    private final int fetchSize = 2000; // tunable

    @Override
    public void executeStream(QueryExecutionPlan plan, Consumer<org.jooq.Record> onRecord) {
        txTemplate.executeWithoutResult(status -> {
            var select = plan.getSelect();
            select.fetchSize(fetchSize);

            try (Stream<org.jooq.Record> stream = select.fetchStream()) {
                stream.forEach(onRecord);
            } catch (RuntimeException ex) {
                log.warn("Stream execution failed", ex);
                throw ex; // will roll back
            }
        });
    }

    @Override
    public <T> List<T> executePage(QueryExecutionPlan plan, Function<org.jooq.Record, T> mapper, int limit) {
        var limited = plan
            .getSelect();
        if (limit > 0) {
            limited.addLimit(limit);
        }

        return dsl.fetch(limited).stream().map(mapper).collect(Collectors.toList());
    }
}
