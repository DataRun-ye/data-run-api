package org.nmcpye.datarun.drun.mongo.domain.datafield;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefaultField extends AbstractField {
    private Object defaultValue;
    private boolean mandatory;
    private boolean readOnly;
    private List<String> appearance;
    private String constraint;
    private Map<String, String> constraintMessage;
    private boolean mainField;

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public List<String> getAppearance() {
        return appearance;
    }

    public void setAppearance(List<String> appearance) {
        this.appearance = appearance;
    }

    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }

    public Map<String, String> getConstraintMessage() {
        return constraint != null ? Objects.requireNonNullElseGet(this.constraintMessage, () -> Map.of("en", "Field with error, check")) : null;
    }

    public void setConstraintMessage(Map<String, String> constraintMessage) {
        this.constraintMessage = constraintMessage;
    }

    public boolean isMainField() {
        return mainField;
    }

    public void setMainField(boolean mainField) {
        this.mainField = mainField;
    }
}
