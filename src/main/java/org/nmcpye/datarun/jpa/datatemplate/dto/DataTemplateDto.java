package org.nmcpye.datarun.jpa.datatemplate.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;

import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.datatemplate.DataTemplate}
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class DataTemplateDto extends BaseDto {
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
