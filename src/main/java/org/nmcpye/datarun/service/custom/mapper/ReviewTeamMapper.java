package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.ReviewTeamDTO;

/**
 * Mapper for the entity {@link ReviewTeam} and its DTO {@link ReviewTeamDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReviewTeamMapper extends EntityMapper<ReviewTeamDTO, ReviewTeam> {
    ReviewTeamMapper INSTANCE = Mappers.getMapper(ReviewTeamMapper.class);
}
