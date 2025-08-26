package org.nmcpye.datarun.jpa.pivot;

/**
 * Aggregation functions supported by the pivot API.
 *
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 24/08/2025
 */
public enum Aggregation {
    SUM,
    AVG,
    COUNT,
    COUNT_DISTINCT,
    MIN,
    MAX,
    SUM_TRUE // convenience for boolean -> sum(true?1:0)
}
