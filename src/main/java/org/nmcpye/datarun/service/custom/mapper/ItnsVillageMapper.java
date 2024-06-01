package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.*;

/**
 * Mapper for the entity {@link ItnsVillage} and its DTO {@link ItnsVillageDTO}.
 */
@Mapper(componentModel = "spring")
public interface ItnsVillageMapper extends EntityMapper<ItnsVillageDTO, ItnsVillage> {
    ItnsVillageMapper INSTANCE = Mappers.getMapper(ItnsVillageMapper.class);

    @Mapping(target = "progressStatus", source = "progressStatus", qualifiedByName = "progressStatusName")
    @Mapping(target = "team", source = "team", qualifiedByName = "teamCode")
    @Mapping(target = "assignment", source = "assignment", qualifiedByName = "assignmentCode")
    @Mapping(target = "activity", source = "activity", qualifiedByName = "activityCode")
    ItnsVillageDTO toDto(ItnsVillage s);

    @Named("progressStatusName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "name", source = "name")
    ProgressStatusDTO toDtoProgressStatusName(ProgressStatus progressStatus);

    @Named("teamCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    TeamDTO toDtoTeamCode(Team team);

    @Named("assignmentCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    AssignmentDTO toDtoAssignmentCode(Assignment assignment);

    @Named("activityCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    ActivityDTO toDtoActivityCode(Activity activity);
}
