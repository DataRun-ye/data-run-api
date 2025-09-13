package org.nmcpye.datarun.analytics.query;

import org.jooq.Record;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Executor interface: separates execution details (fetch size, tx handling, streaming) from building.
 *
 * @author Hamza Assada
 * @since 13/09/2025
 */
public interface QueryExecutor {
    /**
     * Execute plan and stream records to the consumer. The implementation MUST manage connection/transaction
     * lifecycle and close the stream.
     */
    void executeStream(QueryExecutionPlan plan, Consumer<org.jooq.Record> onRecord) throws RuntimeException;

    /**
     * Execute a small page and map to DTOs synchronously. Implementations may add limits.
     */
    <T> List<T> executePage(QueryExecutionPlan plan, Function<Record, T> mapper, int limit) throws RuntimeException;
}
