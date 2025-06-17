package org.nmcpye.datarun.jpa.team.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.dto.TeamDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface TeamMapper extends BaseMapper<TeamDto, Team> {
}
