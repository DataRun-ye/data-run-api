package org.nmcpye.datarun.query.filter;

import lombok.Getter;
import lombok.Value;

import java.util.List;

/**
 * @author Hamza Assada, 23/03/2025
 */
@Value
@Getter
public class CompoundFilter implements FilterExpression {
    LogicalOperator logicalOperator; // AND / OR
    List<FilterExpression> expressions;
}
