package org.nmcpye.datarun.jpa.dataelement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.dataelement.DataTemplateElement;
import org.nmcpye.datarun.jpa.dataelement.dto.DataElementDto;

/**
 * @author Hamza Assada 10/06/2024 <7amza.it@gmail.com>
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataElementMapper extends BaseMapper<DataElementDto, DataTemplateElement> {
}
