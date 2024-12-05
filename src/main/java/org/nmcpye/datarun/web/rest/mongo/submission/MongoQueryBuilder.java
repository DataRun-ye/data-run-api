package org.nmcpye.datarun.web.rest.mongo.submission;

import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MongoQueryBuilder {

    public static Query buildQuery(Map<String, Object> filters) {
        Query query = new Query();

        filters.forEach((key, value) -> {
            String[] parts = key.split("__");
            String field = parts[0];  // The field name (e.g., "status" or "formData.name")
            String operator = parts.length > 1 ? parts[1] : "eq";  // The operator (e.g., "eq", "ne", etc.)

            // Generate criteria based on the operator
            Criteria criteria = switch (operator) {
                case "eq" -> Criteria.where(field).is(value);
                case "ne" -> Criteria.where(field).ne(value);
                case "regex" -> Criteria.where(field).regex(value.toString(), "i");
                case "in" -> Criteria.where(field).in(value instanceof List ? value : Arrays.asList(value.toString().split(",")));
                case "exists" -> Criteria.where(field).exists((Boolean) value);
                case "gt" -> Criteria.where(field).gt(value);
                case "gte" -> Criteria.where(field).gte(value);
                case "lt" -> Criteria.where(field).lt(value);
                case "lte" -> Criteria.where(field).lte(value);
                default -> throw new QueryRequestValidationException("Unsupported operator", field, operator, value.toString());
            };

            query.addCriteria(criteria);
        });

        // Apply security constraints
        applySecurityConstraints(query);

        return query;
    }

    public static Query addProjections(Query query, String fields) {
        if (fields != null) {
            String[] fieldArray = fields.split(",");
            for (String field : fieldArray) {
                query.fields().include(field.trim());
            }
        }
        return query;
    }

    private static void applySecurityConstraints(Query query) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            // No additional constraints for admin
            return;
        }

        Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
        if (currentUserLogin.isPresent()) {
            String login = currentUserLogin.get();
            query.addCriteria(Criteria.where("createdBy").is(login));
        } else {
            // Handle the case where no user is logged in, if necessary
            throw new SecurityException("User is not authenticated");
        }
    }
}

//public class MongoQueryBuilder {
//
//    public static Query buildQuery(Map<String, Object> filters) {
//        Query query = new Query();
//
//        filters.forEach((key, value) -> {
//            String[] parts = key.split("__");
//            String field = parts[0];  // The field name (e.g., "status" or "formData.name")
//            String operator = parts.length > 1 ? parts[1] : "eq";  // The operator (e.g., "eq", "ne", etc.)
//
//            // Generate criteria based on the operator
//            Criteria criteria = switch (operator) {
//                case "eq" -> Criteria.where(field).is(value);
//                case "ne" -> Criteria.where(field).ne(value);
//                case "regex" -> Criteria.where(field).regex(value.toString(), "i");
//                case "in" -> Criteria.where(field).in(value instanceof List ? value : Arrays.asList(value.toString().split(",")));
//                case "exists" -> Criteria.where(field).exists((Boolean) value);
//                case "gt" -> Criteria.where(field).gt(value);
//                case "gte" -> Criteria.where(field).gte(value);
//                case "lt" -> Criteria.where(field).lt(value);
//                case "lte" -> Criteria.where(field).lte(value);
//                default -> throw new QueryRequestValidationException("Unsupported operator", field, operator, value.toString());
//            };
//
//            query.addCriteria(criteria);
//        });
//
//        return query;
//    }
//
//    public static Query addProjections(Query query, String fields) {
//        if (fields != null) {
//            String[] fieldArray = fields.split(",");
//            for (String field : fieldArray) {
//                query.fields().include(field.trim());
//            }
//        }
//        return query;
//    }
//}
