package org.nmcpye.datarun.jpa.entityattribute.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeValue;
import org.nmcpye.datarun.jpa.entityattribute.dto.EntityAttributeValueDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntityAttributeValueMapper extends BaseMapper<EntityAttributeValueDto, EntityAttributeValue> {
    @Override
    @Mapping(source = "entityInstanceId", target = "entityInstance.id")
    EntityAttributeValue toEntity(EntityAttributeValueDto entityAttributeValueDto);

    @Override
    @Mapping(source = "entityInstance.id", target = "entityInstanceId")
    EntityAttributeValueDto toDto(EntityAttributeValue entityAttributeValue);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Override
    @Mapping(source = "entityInstanceId", target = "entityInstance.id")
    EntityAttributeValue partialUpdate(@MappingTarget EntityAttributeValue entityAttributeValue, EntityAttributeValueDto entityAttributeValueDto);
}
