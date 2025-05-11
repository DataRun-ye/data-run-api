package org.nmcpye.datarun.web.rest.mongo.submission;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.Value;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Deprecated(since = "Api v1")
@Value
public class RequestQueryBuilder {
    QueryRequest queryRequest;

    @Deprecated(since = "Api v1")
    public Query buildQuery(Query query) {
        queryRequest.getParsedFilter().forEach((key, value) -> {
            String[] parts = key.split("__");
            String field = parts[0];  // field name
            String operator = parts.length > 1 ? parts[1] : "eq";  // The operator (e.g., "eq", "ne", etc.)

            Criteria criteria = switch (operator) {
                case "eq" -> Criteria.where(field).is(value);
                case "ne" -> Criteria.where(field).ne(value);
                case "regex" -> Criteria.where(field).regex(value.toString(), "i");
                case "in" ->
                    Criteria.where(field).in(value instanceof List ? value : Arrays.asList(value.toString().split(",")));
                case "exists" -> Criteria.where(field).exists((Boolean) value);
                case "gt" -> Criteria.where(field).gt(value);
                case "gte" -> Criteria.where(field).gte(value);
                case "lt" -> Criteria.where(field).lt(value);
                case "lte" -> Criteria.where(field).lte(value);
                default -> throw new QueryRequestValidationException(new ErrorMessage(ErrorCode.E2035, operator));
            };

            query.addCriteria(criteria);
        });

        return query;
    }

    ///
    private Query addProjections(Query query) {
        if (queryRequest.getFields() != null) {
            String[] fieldArray = queryRequest.getFields().split(",");
            for (String field : fieldArray) {
                query.fields().include(field.trim());
            }
        }
        return query;
    }


    @Deprecated(since = "Api v1")
    public Query buildForMongo(Query query) {
        return addProjections(buildQuery(query));
    }

    @Deprecated(since = "Api v1")
    public <E extends AuditableObject<?>> Specification<E> buildForJpa() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            queryRequest.getFilters().forEach((key, value) -> {
                if (key.contains(".")) {
                    // Handle nested properties, for example: parent.uid
                    String[] parts = key.split("\\.");
                    Path<Object> path = root.get(parts[0]);
                    for (int i = 1; i < parts.length; i++) {
                        path = path.get(parts[i]);
                    }
                    predicates.add(cb.equal(path, value));
                } else {
                    // Handle simple properties
                    predicates.add(cb.equal(root.get(key), value));
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
