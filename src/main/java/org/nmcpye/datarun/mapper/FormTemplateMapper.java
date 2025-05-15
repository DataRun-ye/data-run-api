package org.nmcpye.datarun.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.mapper.dto.FormTemplateDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormTemplateMapper
    extends BaseMapper<FormTemplateDto, FormTemplate> {
    @Mappings({
        @Mapping(target = "formVersion", ignore = true),
        @Mapping(target = "versionNumber", ignore = true),
        @Mapping(target = "uid", source = "uid")
    })
    FormTemplate fromSaveDto(SaveFormTemplateDto dto);
}
