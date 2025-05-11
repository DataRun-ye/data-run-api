package org.nmcpye.datarun.dto.formelement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf}
 */
@Value
public class FormDataElementConfDto implements Serializable {
    String id;
    ValueType type;
    String calculation;
    Boolean mandatory;
    Boolean readOnly;
    String constraint;
    Map<String, String> constraintMessage;
    Boolean mainField;
    String optionSet;
    String choiceFilter;
    Boolean gs1Enabled;
    ReferenceType resourceType;
    String resourceMetadataSchema;
    ValueTypeRendering valueTypeRendering;
    String path;
    String parent;
    String code;
    @NotNull
    String name;
    @Size(max = 2000)
    String description;
    Map<String, String> label;
    Integer order;
    List<String> appearance;
}
