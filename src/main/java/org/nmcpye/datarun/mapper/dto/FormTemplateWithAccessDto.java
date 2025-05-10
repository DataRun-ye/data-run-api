package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate}
 */
@Builder
@Value
@AllArgsConstructor
public class FormTemplateWithAccessDto implements Serializable {
    String code;
    @NotNull
    String name;
    @Size(max = 2000)
    String description;
    Boolean disabled;
    boolean deleted;
    Integer version;
    String defaultLocale;
    Map<String, String> label;

    @Size(max = 11)
    String uid;

    List<FormDataElementConfDto> fields;
    List<FormSectionConfDto> sections;

    PermissionsDto permissions;
}
