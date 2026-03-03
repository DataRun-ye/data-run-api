package org.nmcpye.datarun.jpa.datatemplate.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.datatemplate.DataTemplate}
 */
@Value
public class DataTemplateDto {
    @Size(max = 26)
    String id;
    String code;
    @Size(max = 11)
    String uid;
    String versionUid;
    Integer versionNumber;
    Boolean deleted;
    @NotNull
    @NotEmpty(message = "name cannot be empty")
    String name;
    @Size(max = 2000)
    String description;
    Map<String, String> label;
}
