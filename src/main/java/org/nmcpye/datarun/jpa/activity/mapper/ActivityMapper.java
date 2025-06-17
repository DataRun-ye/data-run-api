package org.nmcpye.datarun.jpa.activity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.activity.dto.ActivityDto;

/**
 * @author Hamza Assada 02/04/2025 <7amza.it@gmail.com>
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ActivityMapper extends BaseMapper<ActivityDto, Activity> {
}
