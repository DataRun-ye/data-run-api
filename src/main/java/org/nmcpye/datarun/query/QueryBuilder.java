package org.nmcpye.datarun.query;

import org.nmcpye.datarun.query.filter.FilterExpression;

import java.util.List;

/**
 * @author Hamza, 23/03/2025
 */
public interface QueryBuilder<T> {
    T buildQuery(List<FilterExpression> expressions);
}
