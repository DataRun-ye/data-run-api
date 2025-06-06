package org.nmcpye.datarun.query;

import org.nmcpye.datarun.query.filter.FilterExpression;

import java.util.List;

/**
 * @author Hamza Assada 23/03/2025 <7amza.it@gmail.com>
 */
public interface QueryBuilder<T> {
    T buildQuery(List<FilterExpression> expressions);
}
