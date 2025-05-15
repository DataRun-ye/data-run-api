package org.nmcpye.datarun.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.mapper.dto.FormTemplateDto;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormTemplateMapper
    extends BaseMapper<FormTemplateDto, FormTemplate> {

    @Mappings({
        @Mapping(target = "uid", source = "versionDto.templateUid"),
        @Mapping(target = "currentVersion", source = "versionDto.version"),
    })
    FormTemplate fromVersionDto(FormTemplateVersionDto versionDto);

    FormTemplate fromSaveDto(SaveFormTemplateDto dto);
}
