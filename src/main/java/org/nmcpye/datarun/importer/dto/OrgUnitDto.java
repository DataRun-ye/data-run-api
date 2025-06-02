package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.orgunit.OrgUnit}
 */
@AllArgsConstructor
@Getter
@Setter
public class OrgUnitDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final Set<OrgUnitGroupDto> orgUnitGroups;
}
