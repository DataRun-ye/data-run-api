package org.nmcpye.datarun.analytics.query;

/**
 * CountProvider strategy interface. Implementations can provide exact, approximate or cached counts.
 *
 * @author Hamza Assada
 * @since 13/09/2025
 */
public interface CountProvider {
    /**
     * Return -1 if unable to provide a reliable count.
     */
    long estimate(QueryExecutionPlan plan);
}

