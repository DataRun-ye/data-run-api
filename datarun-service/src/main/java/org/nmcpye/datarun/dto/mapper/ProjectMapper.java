//package org.nmcpye.datarun.drun.postgres.dto.mapper;
//
//import org.mapstruct.*;
//import org.nmcpye.datarun.domain.Project;
//import org.nmcpye.datarun.drun.postgres.dto.ProjectDto;
//
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
//public interface ProjectMapper {
//    Project toEntity(ProjectDto projectDto);
//
//    ProjectDto toDto(Project project);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    Project partialUpdate(ProjectDto projectDto, @MappingTarget Project project);
//}
