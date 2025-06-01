package org.nmcpye.datarun.query;

import org.nmcpye.datarun.query.filter.CompoundFilter;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.query.filter.LogicalOperator;
import org.nmcpye.datarun.query.filter.SimpleFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Hamza Assada, 23/03/2025
 */
@Component("mongoQueryBuilder")
public class MongoQueryBuilder implements QueryBuilder<Query> {

    @Override
    public Query buildQuery(List<FilterExpression> expressions) {
        Criteria criteria = buildCriteria(new CompoundFilter(LogicalOperator.AND, expressions));
        return new Query(criteria);
    }

    public Criteria buildCriteria(FilterExpression expression) {
        if (expression instanceof SimpleFilter simple) {
            return convertSimpleFilterToCriteria(simple);
        } else if (expression instanceof CompoundFilter compound) {
            List<Criteria> subCriteria = compound.getExpressions().stream()
                .map(this::buildCriteria)
                .toList();
            return compound.getLogicalOperator() == LogicalOperator.AND
                ? new Criteria().andOperator(subCriteria.toArray(new Criteria[0]))
                : new Criteria().orOperator(subCriteria.toArray(new Criteria[0]));
        }
        throw new IllegalArgumentException("Unsupported FilterExpression: " + expression);
    }

    private Criteria convertSimpleFilterToCriteria(SimpleFilter filter) {
        String field = filter.getField();
        Object value = filter.getValue();
        return switch (filter.getOperator()) {
            case EQ -> Criteria.where(field).is(value);
            case NE -> Criteria.where(field).ne(value);
            case GT -> Criteria.where(field).gt(value);
            case GTE -> Criteria.where(field).gte(value);
            case LT -> Criteria.where(field).lt(value);
            case LTE -> Criteria.where(field).lte(value);
            case IN -> Criteria.where(field).in((List<?>) value);
            case EXISTS -> Criteria.where(field).exists((Boolean) value);
            case REGEX -> Criteria.where(field).regex(value.toString(), "i");
            case NULL -> Criteria.where(field).is(null);
        };
    }
}

//@Component("mongoQueryBuilder")
//public class MongoQueryBuilder implements QueryBuilder<Query> {
//
//
//    public Criteria buildCriteria(List<FilterExpression> expressions) {
//        Criteria criteria = buildCriteria(new CompoundFilter(LogicalOperator.AND, expressions));
//        return criteria;
//    }
//
//    @Override
//    public Query buildQuery(List<FilterExpression> expressions) {
//        Criteria criteria = buildCriteria(new CompoundFilter(LogicalOperator.AND, expressions));
//        return new Query(criteria);
//    }
//
//    private Criteria buildCriteria(FilterExpression expression) {
//        if (expression instanceof SimpleFilter simple) {
//            return convertSimpleFilterToCriteria(simple);
//        } else if (expression instanceof CompoundFilter compound) {
//            List<Criteria> subCriteria = compound.getExpressions().stream()
//                .map(this::buildCriteria)
//                .toList();
//            if (compound.getLogicalOperator() == LogicalOperator.AND) {
//                return new Criteria().andOperator(subCriteria.toArray(new Criteria[0]));
//            } else {
//                return new Criteria().orOperator(subCriteria.toArray(new Criteria[0]));
//            }
//        }
//        throw new IllegalArgumentException("Unsupported FilterExpression: " + expression);
//    }
//
//    private Criteria convertSimpleFilterToCriteria(SimpleFilter filter) {
//        String field = filter.getField();
//        Object value = filter.getValue();
//        return switch (filter.getOperator()) {
//            case EQ -> Criteria.where(field).is(value);
//            case NE -> Criteria.where(field).ne(value);
//            case GT -> Criteria.where(field).gt(value);
//            case GTE -> Criteria.where(field).gte(value);
//            case LT -> Criteria.where(field).lt(value);
//            case LTE -> Criteria.where(field).lte(value);
//            case IN -> Criteria.where(field).in((List<?>) value);
//            case EXISTS -> Criteria.where(field).exists((Boolean) value);
//            case REGEX -> Criteria.where(field).regex(value.toString(), "i");
//            case NULL -> Criteria.where(field).isNull();
//        };
//    }
//}

