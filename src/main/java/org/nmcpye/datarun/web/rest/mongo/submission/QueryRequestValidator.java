package org.nmcpye.datarun.web.rest.mongo.submission;

import java.util.List;

public class QueryRequestValidator {

    private static final List<String> SUPPORTED_OPERATORS = List.of(
        "eq", "ne", "regex", "in", "exists", "gt", "gte", "lt", "lte"
    );

    public static void validate(QueryRequest request) {
        request.getFilters().forEach((key, value) -> {
            String[] parts = key.split("__");
            if (parts.length == 0) {
                throw new QueryRequestValidationException("Invalid filter format", key, null, value.toString());
            }

            String field = parts[0];
            String operator = parts.length > 1 ? parts[1] : "eq";

            if (!field.matches("^[a-zA-Z0-9._]+$")) {
                throw new QueryRequestValidationException("Invalid field name format", key, operator, value.toString());
            }

            if (!SUPPORTED_OPERATORS.contains(operator)) {
                throw new QueryRequestValidationException("Unsupported operator", key, operator, value.toString());
            }

            validateValue(operator, value, key);
        });

        if (request.getSort() != null && !request.getSort().matches("^[a-zA-Z0-9._]+,(asc|desc)$")) {
            throw new QueryRequestValidationException("Invalid sort format", "sort");
        }

        if (request.getFields() != null && !request.getFields().matches("^[a-zA-Z0-9.,_]+$")) {
            throw new QueryRequestValidationException("Invalid fields format", "fields");
        }
    }

    private static void validateValue(String operator, Object value, String key) {
        switch (operator) {
            case "exists":
                if (!(value instanceof Boolean)) {
                    throw new QueryRequestValidationException("Value for 'exists' must be true or false", key, operator, value.toString());
                }
                break;
            case "in":
                if (!(value instanceof List)) {
                    throw new QueryRequestValidationException("Value for 'in' must be a list", key, operator, value.toString());
                }
                break;
        }
    }
}
