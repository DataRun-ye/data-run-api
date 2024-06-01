package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.*;

/**
 * Mapper for the entity {@link Assignment} and its DTO {@link AssignmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface AssignmentMapper extends EntityMapper<AssignmentDTO, Assignment> {
    AssignmentMapper INSTANCE = Mappers.getMapper(AssignmentMapper.class);

    @Mapping(target = "activity", source = "activity", qualifiedByName = "activityCode")
    @Mapping(target = "organisationUnit", source = "organisationUnit", qualifiedByName = "villageLocationCode")
    @Mapping(target = "team", source = "team", qualifiedByName = "teamCode")
    @Mapping(target = "warehouse", source = "warehouse", qualifiedByName = "warehouseName")
    AssignmentDTO toDto(Assignment s);

    @Named("activityCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    ActivityDTO toDtoActivityCode(Activity activity);

    @Named("villageLocationCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    VillageLocationDTO toDtoVillageLocationCode(VillageLocation villageLocation);

    @Named("teamCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    TeamDTO toDtoTeamCode(Team team);

    @Named("warehouseName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "name", source = "name")
    WarehouseDTO toDtoWarehouseName(Warehouse warehouse);
}
