package org.nmcpye.datarun.datatemplateelement;

import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Getter
@Setter
public class FormDataElementConf extends AbstractElement {
    private String id;
    @Field("type")
    private ValueType type;
    private String calculation;
    private Object defaultValue;
    private Boolean mandatory;
    private Boolean readOnly;
    private ElementValidationRule validationRule;
    private Boolean mainField;
    private String optionSet;
    private String choiceFilter;
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
    private ScannedCodeProperties properties;
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

    @Override
    public FormDataElementConf path(String path) {
        this.setPath(path);
        return this;
    }

    public FormDataElementConf type(ValueType type) {
        this.setType(type);
        return this;
    }
}
