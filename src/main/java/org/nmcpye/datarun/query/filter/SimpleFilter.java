package org.nmcpye.datarun.query.filter;

import lombok.Getter;
import lombok.Value;

/**
 * @author Hamza Assada 23/03/2025 <7amza.it@gmail.com>
 */
@Value
@Getter
public class SimpleFilter implements FilterExpression {
    String field;
    FilterOperator operator;
    Object value;
}
