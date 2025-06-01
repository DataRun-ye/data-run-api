package org.nmcpye.datarun.web.rest.mongo.submission;

import lombok.AllArgsConstructor;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.*;

@AllArgsConstructor
@Deprecated(since = "Api v1")
public class RequestQueryBuilder {
    QueryRequest queryRequest;

    @Deprecated(since = "Api v1")
    public Set<Criteria> buildMongoCriteria() {
        Set<Criteria> queryCriteriaDefinitions = new HashSet<>();
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

            queryCriteriaDefinitions.add(criteria);
        });

        return queryCriteriaDefinitions;
    }

    ///
    public Set<String> projection(QueryRequest queryRequest) {
        if (queryRequest.getFields() != null) {
            Set<String> projections = new HashSet<>();
            String[] fieldArray = queryRequest.getFields().split(",");
            for (String field : fieldArray) {
                projections.add(field.trim());
            }
            return projections;
        }
        return Collections.emptySet();
    }
}
