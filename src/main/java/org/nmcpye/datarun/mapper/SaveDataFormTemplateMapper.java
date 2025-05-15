package org.nmcpye.datarun.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface SaveDataFormTemplateMapper
    extends BaseMapper<SaveFormTemplateDto, FormTemplate> {
    @Mappings({
        @Mapping(target = "uid", source = "master.uid"),
        @Mapping(target = "disabled", source = "master.disabled"),
        @Mapping(target = "deleted", source = "master.deleted"),
        @Mapping(target = "code", source = "master.code"),
        @Mapping(target = "name", source = "master.name"),
        @Mapping(target = "description", source = "master.description"),
        @Mapping(target = "defaultLocale", source = "master.defaultLocale"),
        @Mapping(target = "label", source = "master.label"),
        @Mapping(target = "versionNumber", source = "master.versionNumber"),
        @Mapping(target = "formVersion", source = "master.formVersion"),
        @Mapping(target = "fields", source = "version.fields"),
        @Mapping(target = "sections", source = "version.sections"),
    })
    SaveFormTemplateDto combineMasterAndVersion(FormTemplate master, FormTemplateVersion version);
}
