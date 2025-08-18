package org.nmcpye.datarun.datatemplateelement;

import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class FormDataElementConf extends AbstractElement {
    private String id;

    @Field("type")
    private ValueType type;
    private String calculation;
    private Object defaultValue;
    private Boolean mandatory = Boolean.FALSE;
    private Boolean showInSummary = Boolean.FALSE;
    private String optionSet;
    private String choiceFilter;
    private ElementValidationRule validationRule;
    /**
     * Deprecated, use {@link ElementValidationRule}'s expression instead
     */
    @Deprecated(since = "V7")
    String constraint;
    /**
     * Deprecated, use {@link ElementValidationRule}'s message instead
     */
    @Deprecated(since = "V7")
    Map<String, String> constraintMessage;
    private Boolean gs1Enabled;

    private ScannedCodeProperties scannedCodeProperties;
    /**
     * resourceType for ReferenceField type
     */
    private ReferenceType resourceType;
    /**
     * resourceMetadataSchema for ReferenceField type
     */
    private String resourceMetadataSchema;
    private ValueTypeRendering valueTypeRendering = ValueTypeRendering.DEFAULT;
    private AggregationType aggregationType = AggregationType.DEFAULT;

    public Boolean isMultiSelect() {
        return this.type.isOptionsType() ? this.type == ValueType.SelectMulti : null;
    }

    public Boolean getMainField() {
        return showInSummary;
    }

    @Override
    public FormDataElementConf path(String path) {
        this.setPath(path);
        return this;
    }

    public FormDataElementConf type(ValueType type) {
        this.setType(type);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormDataElementConf that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
