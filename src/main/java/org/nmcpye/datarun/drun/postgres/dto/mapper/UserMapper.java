package org.nmcpye.datarun.drun.postgres.dto.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.dto.UserDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
//    @Mapping(target = "id", source = "id")
    User toEntity(UserDto userDto);

//    @Mapping(target = "id", source = "id")
    UserDto toDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User partialUpdate(UserDto userDto, @MappingTarget User user);
}
