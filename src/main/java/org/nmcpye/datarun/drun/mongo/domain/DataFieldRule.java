package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.RuleAction;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * A DataFieldRule.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFieldRule implements Serializable {

    private static final long serialVersionUID = 1L;

//    @Size(max = 11)
//    @Field("uid")
//    private String uid;

    @NotNull
    @Field("expression")
    private String expression;

    @NotNull
    @Field("action")
    private RuleAction action;

    @Field("message")
    private Map<String, String> message;

    private FilterRuleInfo filterInfo;

    private String assignedValue;

    public String getAssignedValue() {
        return assignedValue;
    }

    public void setAssignedValue(String assignedValue) {
        this.assignedValue = assignedValue;
    }

    public Map<String, String> getMessage() {
        return message;
    }


    public void setMessage(Map<String, String> message) {
        this.message = Objects.requireNonNullElseGet(message, () -> Map.of("en", "Error"));
    }

    public String getExpression() {
        return this.expression;
    }

    public DataFieldRule expression(String expression) {
        this.setExpression(expression);
        return this;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public RuleAction getAction() {
        return this.action;
    }

    public DataFieldRule action(RuleAction action) {
        this.setAction(action);
        return this;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public FilterRuleInfo getFilterInfo() {
        return filterInfo;
    }

    public void setFilterInfo(FilterRuleInfo filterInfo) {
        this.filterInfo = filterInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataFieldRule that = (DataFieldRule) o;
        return Objects.equals(expression, that.expression) && action == that.action && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, action, message);
    }
}
