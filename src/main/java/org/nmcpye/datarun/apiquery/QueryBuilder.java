package org.nmcpye.datarun.apiquery;

import org.nmcpye.datarun.apiquery.filter.FilterExpression;

import java.util.List;

/**
 * @author Hamza Assada 23/03/2025 (7amza.it@gmail.com)
 */
public interface QueryBuilder<T> {
    T buildQuery(List<FilterExpression> expressions);
}
