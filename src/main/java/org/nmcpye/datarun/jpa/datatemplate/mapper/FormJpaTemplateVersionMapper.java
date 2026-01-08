package org.nmcpye.datarun.jpa.datatemplate.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormJpaTemplateVersionMapper
    extends BaseMapper<FormTemplateVersionDto, TemplateVersion> {

    @Mappings({
        @Mapping(target = "uid", ignore = true),
//        @Mapping(target = "id", ignore = true),
        @Mapping(target = "versionNumber", ignore = true),
        @Mapping(target = "templateUid", source = "uid"),
    })
    TemplateVersion fromInstanceDto(DataTemplateInstanceDto dto);
}
