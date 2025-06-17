package org.nmcpye.datarun.jpa.team.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.team.Team}
 *
 * @author Hamza Assada 06/06/2025 <7amza.it@gmail.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TeamDto extends BaseDto {
    private String description;
    private BaseDto activity;
    private Set<BaseDto> users = new HashSet<>();
}
