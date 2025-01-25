package org.nmcpye.datarun.mongo.domain.dataelement;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

public class FormDataElementConf extends FormElementConf {

    private String id;

    @NotNull
    @Field("type")
    private ValueType type;

    private String calculation;
    private Object defaultValue;
    private Boolean mandatory;
    private Boolean readOnly;
    private String constraint;
    private Map<String, String> constraintMessage;
    private Boolean mainField;
    private String optionSet;
    private String choiceFilter;
    private Boolean gs1Enabled;
    private ScannedCodeProperties properties;
    /**
     * resourceType for ReferenceField type
     */
    private ReferenceType resourceType;

    // TODO rename to referenceMetadataSchema
    /**
     * resourceMetadataSchema for ReferenceField type
     */
    private String resourceMetadataSchema;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public FormDataElementConf id(String id) {
        this.setId(id);
        return this;
    }


    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public String getCalculation() {
        return calculation;
    }

    public void setCalculation(String calculation) {
        this.calculation = calculation;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }

    public Map<String, String> getConstraintMessage() {
        return constraintMessage;
    }

    public void setConstraintMessage(Map<String, String> constraintMessage) {
        this.constraintMessage = constraintMessage;
    }

    public Boolean getMainField() {
        return mainField;
    }

    public void setMainField(Boolean mainField) {
        this.mainField = mainField;
    }

    public String getOptionSet() {
        return optionSet;
    }

    public void setOptionSet(String optionSet) {
        this.optionSet = optionSet;
    }

    public String getChoiceFilter() {
        return choiceFilter;
    }

    public void setChoiceFilter(String choiceFilter) {
        this.choiceFilter = choiceFilter;
    }

    public Boolean getGs1Enabled() {
        return gs1Enabled;
    }

    public void setGs1Enabled(Boolean gs1Enabled) {
        this.gs1Enabled = gs1Enabled;
    }

    public ScannedCodeProperties getProperties() {
        return properties;
    }

    public void setProperties(ScannedCodeProperties properties) {
        this.properties = properties;
    }

    public ReferenceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ReferenceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceMetadataSchema() {
        return resourceMetadataSchema;
    }

    public void setResourceMetadataSchema(String resourceMetadataSchema) {
        this.resourceMetadataSchema = resourceMetadataSchema;
    }
}
