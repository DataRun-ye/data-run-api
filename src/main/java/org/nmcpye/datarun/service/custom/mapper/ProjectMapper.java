package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.ProjectDTO;

/**
 * Mapper for the entity {@link Project} and its DTO {@link ProjectDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProjectMapper extends EntityMapper<ProjectDTO, Project> {
    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);
}
