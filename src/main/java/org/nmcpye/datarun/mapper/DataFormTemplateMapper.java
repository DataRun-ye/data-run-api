package org.nmcpye.datarun.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataFormTemplateMapper
    extends BaseMapper<SaveFormTemplateDto, DataFormTemplate> {
//    @Mappings({
//        @Mapping(target = "uid", source = "templateUid"),
//    })
//    DataFormTemplate fromVersionDto(FormTemplateVersionDto versionDto);

    @Mapping(target = "version", source = "versionNumber")
    @Override
    DataFormTemplate toEntity(SaveFormTemplateDto dto);

    @Mapping(target = "versionNumber", source = "version")
    @Override
    SaveFormTemplateDto toDto(DataFormTemplate entity);
}
