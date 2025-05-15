package org.nmcpye.datarun.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormTemplateVersionMapper
    extends BaseMapper<FormTemplateVersionDto, FormTemplateVersion> {

    @Mappings({
        @Mapping(target = "templateUid", source = "uid", defaultExpression = "java(org.nmcpye.datarun.utils.CodeGenerator.generateUid())"),
        @Mapping(target = "label", source = "label",
            defaultExpression = "java(Map.of(\"en\", dto.getName(), \"ar\", dto.getName()))"),
        @Mapping(target = "defaultLocale", source = "defaultLocale", defaultValue = "ar"),
    })
    FormTemplateVersion fromSaveDto(SaveFormTemplateDto dto);

    @Mappings({
        @Mapping(target = "uid", source = "version.uid"),
        @Mapping(target = "templateUid", source = "master.uid"),
        @Mapping(target = "disabled", source = "master.disabled"),
        @Mapping(target = "deleted", source = "master.deleted"),
        @Mapping(target = "code", source = "version.code"),
        @Mapping(target = "name", source = "version.name"),
        @Mapping(target = "description", source = "version.description"),
        @Mapping(target = "version", source = "version.version"),
        @Mapping(target = "defaultLocale", source = "version.defaultLocale"),
        @Mapping(target = "label", source = "version.label"),
        @Mapping(target = "fields", source = "version.fields"),
        @Mapping(target = "sections", source = "version.sections"),
    })
    FormTemplateVersionDto combineMasterAndVersion(FormTemplate master, FormTemplateVersion version);
}
