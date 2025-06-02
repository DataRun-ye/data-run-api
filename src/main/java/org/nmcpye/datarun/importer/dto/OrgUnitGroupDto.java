package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.orgunitgroup.OrgUnitGroup}
 */
@AllArgsConstructor
@Getter
@Setter
public class OrgUnitGroupDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final String symbol;
    private final String color;
    private final Boolean inactive;
    private final Set<OrgUnitGroupSetDto> orgUnitGroupSets;
}
