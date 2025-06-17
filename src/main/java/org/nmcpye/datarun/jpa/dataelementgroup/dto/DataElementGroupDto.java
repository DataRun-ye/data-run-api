package org.nmcpye.datarun.jpa.dataelementgroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DataElementGroupDto extends BaseDto {
    private String name;
    private Set<BaseDto> dataElements = new HashSet<>();
    private Set<BaseDto> dataElementGroupSets = new HashSet<>();
}
