package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf}
 */
@Getter
@Setter
public class FormDataElementConfDto extends FormElementConf {
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
