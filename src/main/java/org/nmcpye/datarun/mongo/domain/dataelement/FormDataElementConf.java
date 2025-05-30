package org.nmcpye.datarun.mongo.domain.dataelement;

import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Getter
@Setter
public class FormDataElementConf extends FormElementConf {

    private String id;

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

//    /**
//     *  for referenceType ReferenceType.Stage
//     */
//    private StageReference stageReference;

    // TODO rename to referenceMetadataSchema
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
