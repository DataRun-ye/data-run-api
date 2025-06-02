package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataElementGroupSetDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
}
