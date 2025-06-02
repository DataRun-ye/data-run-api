package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.datatemplate.DataTemplate}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataTemplateDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    @NotNull
    private final String versionUid;
    @NotNull
    private final Integer versionNumber;
    private final Boolean deleted;
    private final String description;
}
