package org.nmcpye.datarun.dto.formelement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;
import org.nmcpye.datarun.dto.formrule.DataFieldRuleDto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf}
 */
@Value
public class FormSectionConfDto implements Serializable {
    Boolean repeatable;
    String path;
    String parent;
    String code;
    @NotNull
    String name;
    @Size(max = 2000)
    String description;
    Map<String, String> label;
    List<DataFieldRuleDto> rules;
    Integer order;
    List<String> appearance;
}
