package org.nmcpye.datarun.common;

import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class QueryRequest {

    private Map<String, Object> filters = new HashMap<>();

    private String fields;

    private boolean paged = true;

    private int page = 0;

    private int size = 20;
    private String sort;

    private boolean flatten = false;

    private boolean aggregate = false;

    private boolean includeDeleted = false;

    private boolean includeDisabled = false;

    private boolean eagerload = false;

    private boolean mergeElements = false;

    public Map<String, Object> getParsedFilter() {
        return filters.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> parseValue(entry.getKey(), entry.getValue())
        ));
    }

    ///

    public Pageable getPageable() {
        if (!isPaged()) {
            return Pageable.unpaged();
        }

        return PageRequest.of(getPage(), getSize());
    }

    public int getSize() {
        if (!isPaged()) {
            return Integer.MAX_VALUE;
        }
        return size;
    }

    public int getPage() {
        if (!isPaged()) {
            return 0;
        }
        return page;
    }

    private Object parseValue(String key, Object value) {
        if (value instanceof String strValue) {
            try {
                if (key.endsWith("__eq") || key.endsWith("__ne")) {
                    return parseTypedValue(strValue);
                } else if (key.endsWith("__gt") || key.endsWith("__gte") ||
                    key.endsWith("__lt") || key.endsWith("__lte")) {
                    return parseNumber(strValue);
                } else if (key.endsWith("__exists")) {
                    return Boolean.parseBoolean(strValue);
                } else if (key.endsWith("__in")) {
                    return strValue.split(",");
                } else if (key.endsWith("__regex")) {
                    return strValue;
                }
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2014, "Failed to parse filter value for key: " + key + " " + e);
            }
        }
        return value;
    }

    private Object parseTypedValue(String value) {
        try {
            if (value.matches("-?\\d+")) {
                return Integer.parseInt(value);
            } else if (value.matches("-?\\d+\\.\\d+")) {
                return Double.parseDouble(value);
            } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            } else {
                // Default to String
                return value;
            }
        } catch (NumberFormatException e) {
            throw new IllegalQueryException(ErrorCode.E2014, "Invalid value format: " + value);
        }
    }

    private Number parseNumber(String value) {
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalQueryException(ErrorCode.E2014, "Invalid numeric value: " + value);
        }
    }

}
