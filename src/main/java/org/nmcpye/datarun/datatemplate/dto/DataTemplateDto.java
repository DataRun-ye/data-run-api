package org.nmcpye.datarun.datatemplate.dto;

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
public class DataTemplateDto implements Serializable {
    @Size(max = 11)
    String uid;
    String versionUid;
    Integer versionNumber;
    Boolean deleted;
    String code;
    @NotNull
    @NotEmpty(message = "name cannot be empty")
    String name;
    @Size(max = 2000)
    String description;
    Map<String, String> label;
}
