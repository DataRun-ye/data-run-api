package org.nmcpye.datarun.web.rest.mongo.submission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class QueryRequest {

    // Filters: Supports field-based filtering with operators
    @Schema(description = "Filters in format field__operator=value, e.g., status__eq=completed. " +
        "Supported operators: " +
        "eq (equal), " +
        "ne (not equal), " +
        "regex (regular expression), " +
        "in (in list), " +
        "exists (field existence), " +
        "gt (greater than), " +
        "gte (greater than or equal to), " +
        "lt (less than or equal to)")
    private Map<String, Object> filters = new HashMap<>();

    // Pagination parameters
    @Schema(description = "Page number (default: 0)")
    private int page = 0;

    @Schema(description = "Page size (default: 10)")
    private int size = 10;

    // Sorting parameters
    @Schema(description = "Sort fields in format field,direction, e.g., finishedEntryTime,asc")
    private String sort;

    // Projection: Specify fields to include in the response
    @Schema(description = "Comma-separated list of fields to include, e.g., id,uid,status")
    private String fields;

    // Advanced aggregation flag
    @Schema(description = "Flag to enable advanced aggregation queries")
    private boolean aggregate = false;

    // Flattening option
    @Schema(description = "Flag to enable flattening of nested objects")
    private boolean flatten = false;

    // Flattening option
    @Schema(description = "Flag to enable fetching paged or not paged content")
    private boolean paged = true;

    // Utility for filters parsing
    public Map<String, Object> parseFilters() {
        return filters.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey, Map.Entry::getValue
        ));
    }

    public static Map<String, Object> parseFilter(String filter) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(filter, new TypeReference<Map<String, Object>>() {
        });
    }

    public int getSize() {
        if (!paged) {
            return Integer.MAX_VALUE;
        }
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPage() {
        if (!paged) {
            return 0;
        }
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }


    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }

    public boolean isAggregate() {
        return aggregate;
    }

    public void setAggregate(boolean aggregate) {
        this.aggregate = aggregate;
    }

    public boolean isFlatten() {
        return flatten;
    }

    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }

    public boolean isPaged() {
        return paged;
    }

    public void setPaged(boolean paged) {
        this.paged = paged;
    }
}
