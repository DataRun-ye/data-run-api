package org.nmcpye.datarun.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataFormTemplateMapper
    extends BaseMapper<DataTemplateInstanceDto, DataFormTemplate> {
    @Mapping(target = "version", source = "versionNumber")
    @Override
    DataFormTemplate toEntity(DataTemplateInstanceDto dto);

    @Mapping(target = "versionNumber", source = "version")
    @Override
    DataTemplateInstanceDto toDto(DataFormTemplate entity);
}
