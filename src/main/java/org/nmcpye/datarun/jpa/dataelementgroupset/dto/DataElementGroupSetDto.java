package org.nmcpye.datarun.jpa.dataelementgroupset.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DataElementGroupSetDto extends BaseDto {
    private String name;
    private Set<BaseDto> dataElementGroups = new HashSet<>();
}
