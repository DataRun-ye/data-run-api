package org.nmcpye.datarun.mongo.datatemplateversion.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormTemplateVersionMapper
    extends BaseMapper<FormTemplateVersionDto, DataTemplateVersion> {

    @Mappings({
        @Mapping(target = "uid", ignore = true),
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "versionNumber", ignore = true),
    })
    DataTemplateVersion fromInstanceDto(DataTemplateInstanceDto dto);
}
