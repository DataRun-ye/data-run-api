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
//        @Mapping(target = "id", expression = "java(getVersionId(dto))"),
        @Mapping(target = "uid", ignore = true),
        @Mapping(target = "versionNumber", ignore = true),
    })
    FormTemplateVersion fromSaveDto(SaveFormTemplateDto dto);

//    default String getVersionId(SaveFormTemplateDto dto) {
//        return dto.getUid() + "_" + dto.getVersionNumber();
//    }
}
