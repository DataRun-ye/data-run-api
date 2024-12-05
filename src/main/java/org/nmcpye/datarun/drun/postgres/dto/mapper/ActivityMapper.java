//package org.nmcpye.datarun.drun.postgres.dto.mapper;
//
//import org.mapstruct.*;
//import org.nmcpye.datarun.domain.Activity;
//import org.nmcpye.datarun.drun.postgres.dto.ActivityDto;
//
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
//    componentModel = MappingConstants.ComponentModel.SPRING)
//public interface ActivityMapper {
//    Activity toEntity(ActivityDto activityDto);
//
//    ActivityDto toDto(Activity activity);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    Activity partialUpdate(ActivityDto activityDto, @MappingTarget Activity activity);
//}
