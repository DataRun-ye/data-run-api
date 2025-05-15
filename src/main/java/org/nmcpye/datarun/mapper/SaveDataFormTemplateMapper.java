//package org.nmcpye.datarun.mapper;
//
//import org.mapstruct.*;
//import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
//import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
//import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
//
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
//    componentModel = MappingConstants.ComponentModel.SPRING)
//public interface SaveDataFormTemplateMapper
//    extends BaseMapper<SaveFormTemplateDto, DataFormTemplate> {
//    @Mappings({
//        @Mapping(target = "uid", source = "templateUid"),
//    })
//    DataFormTemplate fromVersionDto(FormTemplateVersionDto formTemplateVersionDto);
//}
