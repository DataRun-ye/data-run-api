package org.nmcpye.datarun.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormTemplateVersionMapper
    extends BaseMapper<FormTemplateVersionDto, FormTemplateVersion> {

    @Mappings({
        @Mapping(target = "templateUid", source = "uid"),
        @Mapping(target = "label", source = "label",
            defaultExpression = "java(Map.of(\"en\", dto.getName(), \"ar\", dto.getName()))"),
        @Mapping(target = "defaultLocale", source = "defaultLocale", defaultValue = "ar"),
    })
    FormTemplateVersion fromSaveDto(SaveFormTemplateDto dto);
}
