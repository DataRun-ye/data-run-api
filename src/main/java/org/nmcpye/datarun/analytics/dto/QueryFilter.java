package org.nmcpye.datarun.analytics.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;


/// Simple Query filter DTO.
///
/// @author Hamza Assada
/// @since 27/08/2025
@Builder
@Data
@Accessors(fluent = true)
public final class QueryFilter {
    /// either an MV column name ("team_uid", "value_num", "option_uid") or a measure alias.
    ///
    /// * When field references element id forms (`etc:<uid>` or `de:<uid>`) the system will convert that
    ///  to the appropriate schema column and value.
    ///
    /// * If field is an alias, the builder resolves alias -> expression if possible. Prefer using MV columns for filters to avoid ambiguity.
    private final String field;

    /// operator: one of `["=", "!=", "IN", ">", "<", ">=", "<=", "LIKE", "ILIKE"]`.
    private final String op;

    /// RHS value or collection (for IN)
    private final Object value;

    public QueryFilter(String field, String op, Object value) {
        this.field = field;
        this.op = op;
        this.value = value;
    }

    public String field() {
        return field;
    }

    public String op() {
        return op;
    }

    public Object value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (QueryFilter) obj;
        return Objects.equals(this.field, that.field) &&
            Objects.equals(this.op, that.op) &&
            Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, op, value);
    }

    @Override
    public String toString() {
        return "QueryFilter[" +
            "field=" + field + ", " +
            "op=" + op + ", " +
            "value=" + value + ']';
    }

}
