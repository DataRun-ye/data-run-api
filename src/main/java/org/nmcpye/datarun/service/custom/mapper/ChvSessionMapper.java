package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.ChvSessionDTO;
import org.nmcpye.datarun.service.custom.dto.TeamDTO;

/**
 * Mapper for the entity {@link ChvSession} and its DTO {@link ChvSessionDTO}.
 */
@Mapper(componentModel = "spring")
public interface ChvSessionMapper extends EntityMapper<ChvSessionDTO, ChvSession> {
    ChvSessionMapper INSTANCE = Mappers.getMapper(ChvSessionMapper.class);

    @Mapping(target = "team", source = "team", qualifiedByName = "teamCode")
    ChvSessionDTO toDto(ChvSession s);

    @Named("teamCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    TeamDTO toDtoTeamCode(Team team);
}
