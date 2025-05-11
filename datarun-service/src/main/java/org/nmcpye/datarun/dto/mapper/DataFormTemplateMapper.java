package org.nmcpye.datarun.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.mapper.dto.FormTemplateWithAccessDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataFormTemplateMapper
    extends BaseMapper<FormTemplateWithAccessDto, DataFormTemplate> {
}
