package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.FormTemplate}
 */
@Value
public class FormTemplateDto implements Serializable {
    @Size(max = 11)
    String uid;
    String formVersion;
    Integer versionNumber;
    Boolean disabled;
    Boolean deleted;
    String code;
    @NotNull
    @NotEmpty(message = "name cannot be empty")
    String name;
    @Size(max = 2000)
    String description;
    String defaultLocale;
    Map<String, String> label;
}
