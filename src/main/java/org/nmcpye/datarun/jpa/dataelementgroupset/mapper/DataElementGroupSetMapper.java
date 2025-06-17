package org.nmcpye.datarun.jpa.dataelementgroupset.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet;
import org.nmcpye.datarun.jpa.dataelementgroupset.dto.DataElementGroupSetDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataElementGroupSetMapper extends BaseMapper<DataElementGroupSetDto, DataElementGroupSet> {
}
