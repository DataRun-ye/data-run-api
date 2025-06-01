package org.nmcpye.datarun.datatemplateversion.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.datatemplateversion.DataTemplateTemplateVersion;
import org.nmcpye.datarun.datatemplateversion.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.BaseMapper;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormTemplateVersionMapper
    extends BaseMapper<FormTemplateVersionDto, DataTemplateTemplateVersion> {

    @Mappings({
        @Mapping(target = "uid", ignore = true),
        @Mapping(target = "versionNumber", ignore = true),
    })
    DataTemplateTemplateVersion fromInstanceDto(DataTemplateInstanceDto dto);

//    @Mappings({
//        @Mapping(target = "formVersion", source = "uid"),
//        @Mapping(target = "uid", source = "templateUid"),
//        @Mapping(target = "versionNumber", source = "versionNumber"),
//    })
//    SaveFormTemplateDto toSaveDto(FormTemplateVersion version);
}
