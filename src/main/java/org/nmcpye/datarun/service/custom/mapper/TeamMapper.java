package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.ActivityDTO;
import org.nmcpye.datarun.service.custom.dto.ReviewTeamDTO;
import org.nmcpye.datarun.service.custom.dto.TeamDTO;
import org.nmcpye.datarun.service.custom.dto.WarehouseDTO;
import org.nmcpye.datarun.service.dto.UserDTO;

/**
 * Mapper for the entity {@link Team} and its DTO {@link TeamDTO}.
 */
@Mapper(componentModel = "spring")
public interface TeamMapper extends EntityMapper<TeamDTO, Team> {
    TeamMapper INSTANCE = Mappers.getMapper(TeamMapper.class);

    @Mapping(target = "activity", source = "activity", qualifiedByName = "activityCode")
    @Mapping(target = "operationRoom", source = "operationRoom", qualifiedByName = "reviewTeamName")
    @Mapping(target = "warehouse", source = "warehouse", qualifiedByName = "warehouseName")
    @Mapping(target = "userInfo", source = "userInfo", qualifiedByName = "userLogin")
    TeamDTO toDto(Team s);

    @Named("activityCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    ActivityDTO toDtoActivityCode(Activity activity);

    @Named("reviewTeamName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "name", source = "name")
    ReviewTeamDTO toDtoReviewTeamName(ReviewTeam reviewTeam);

    @Named("warehouseName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "name", source = "name")
    WarehouseDTO toDtoWarehouseName(Warehouse warehouse);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
