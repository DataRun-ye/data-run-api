//package org.nmcpye.datarun.drun.postgres.dto.mapper;
//
//import org.mapstruct.*;
//import org.nmcpye.datarun.assignment.Assignment;
//import org.nmcpye.datarun.drun.postgres.dto.AssignmentDto;
//
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
//public interface AssignmentMapper {
//    Assignment toEntity(AssignmentDto assignmentDto);
//
//    AssignmentDto toDto(Assignment assignment);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    Assignment partialUpdate(AssignmentDto assignmentDto, @MappingTarget Assignment assignment);
//}
