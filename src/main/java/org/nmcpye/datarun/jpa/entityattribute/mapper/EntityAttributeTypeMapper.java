package org.nmcpye.datarun.jpa.entityattribute.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeType;
import org.nmcpye.datarun.jpa.entityattribute.dto.EntityAttributeTypeDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntityAttributeTypeMapper extends BaseMapper<EntityAttributeTypeDto, EntityAttributeType> {
}
