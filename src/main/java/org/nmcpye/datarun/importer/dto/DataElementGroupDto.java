package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataElementGroupDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final Set<DataElementGroupSetDto> dataElementGroupSets;
}
