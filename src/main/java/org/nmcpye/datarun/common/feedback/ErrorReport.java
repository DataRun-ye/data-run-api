package org.nmcpye.datarun.common.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ErrorReport {
    protected final ErrorMessage message;

    protected final Class<?> mainKlass;

    protected String mainId;

    protected Class<?> errorKlass;

    protected String errorProperty;

    protected List<Object> errorProperties = new ArrayList<>();

    protected Object value;

    public ErrorReport(Class<?> mainKlass, ErrorCode errorCode, Object... args) {
        this.mainKlass = mainKlass;
        this.message = new ErrorMessage(errorCode, args);
        this.errorProperties.addAll(Arrays.asList(args));
    }

    public ErrorReport(Class<?> mainKlass, ErrorMessage message) {
        this.mainKlass = mainKlass;
        this.message = message;
    }

    @JsonCreator
    public ErrorReport(
        @JsonProperty("message") String message,
        @JsonProperty("mainKlass") Class<?> mainKlass,
        @JsonProperty("errorCode") ErrorCode errorCode) {
        this.mainKlass = mainKlass;
        this.message = new ErrorMessage(message, errorCode);
    }

    @JsonProperty
    public ErrorCode getErrorCode() {
        return message.getErrorCode();
    }

    @JsonProperty
    public String getMessage() {
        return message.getMessage();
    }

    @JsonProperty
    public Class<?> getMainKlass() {
        return mainKlass;
    }

    @JsonProperty
    public String getMainId() {
        return mainId;
    }

    public ErrorReport setMainId(String mainId) {
        this.mainId = mainId;
        return this;
    }

    @JsonProperty
    public Class<?> getErrorKlass() {
        return errorKlass;
    }

    public ErrorReport setErrorKlass(Class<?> errorKlass) {
        this.errorKlass = errorKlass;
        return this;
    }

    @JsonProperty
    public String getErrorProperty() {
        return errorProperty;
    }

    public ErrorReport setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
        return this;
    }

    @JsonProperty
    public List<Object> getErrorProperties() {
        return errorProperties;
    }

    public void setErrorProperties(List<Object> errorProperties) {
        this.errorProperties = errorProperties;
    }

    @JsonProperty
    public Object getValue() {
        return value;
    }

    public ErrorReport setValue(Object value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("message", getMessage())
            .add("errorCode", message.getErrorCode())
            .add("mainKlass", mainKlass)
            .add("errorKlass", errorKlass)
            .add("value", value)
            .toString();
    }
}
