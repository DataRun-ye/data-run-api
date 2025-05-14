package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate}
 */
@Getter
@Setter
@Value
@AllArgsConstructor
public class FormTemplateVersionDto implements Serializable {

    @NotNull
    String uid;

    @Size(max = 11)
    @NotNull
    String templateUid;

    String code;

    @NotNull
    String name;

    @Size(max = 2000)
    String description;

    Integer version;

    Boolean disabled;

    Boolean deleted;


    String defaultLocale;

    Map<String, String> label;

    List<FormDataElementConfDto> fields;
    List<FormSectionConfDto> sections;
}
