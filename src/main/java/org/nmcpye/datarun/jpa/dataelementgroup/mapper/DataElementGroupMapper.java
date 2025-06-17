package org.nmcpye.datarun.jpa.dataelementgroup.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.jpa.dataelementgroup.dto.DataElementGroupDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataElementGroupMapper extends BaseMapper<DataElementGroupDto, DataElementGroup> {
}
