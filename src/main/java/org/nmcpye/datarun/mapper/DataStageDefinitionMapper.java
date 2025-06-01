package org.nmcpye.datarun.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.datastage.DataStageDefinition;
import org.nmcpye.datarun.mapper.dto.DataStageDefinitionDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataStageDefinitionMapper extends BaseMapper<DataStageDefinitionDto, DataStageDefinition> {
    @Override
    @Mappings({
        @Mapping(source = "assignmentTypeUid", target = "assignmentType.uid"),
        @Mapping(source = "dataTemplateUid", target = "dataTemplate.uid")
    })
    DataStageDefinition toEntity(DataStageDefinitionDto dataStageDefinitionDto);

    @Override
    @Mappings({
        @Mapping(source = "assignmentType.uid", target = "assignmentTypeUid"),
        @Mapping(source = "dataTemplate.uid", target = "dataTemplateUid")
    })
    DataStageDefinitionDto toDto(DataStageDefinition dataStageDefinition);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
        @Mapping(source = "assignmentTypeUid", target = "assignmentType.uid"),
        @Mapping(source = "dataTemplateUid", target = "dataTemplate.uid")
    })
    DataStageDefinition partialUpdate(DataStageDefinitionDto dataStageDefinitionDto, @MappingTarget DataStageDefinition dataStageDefinition);
}
