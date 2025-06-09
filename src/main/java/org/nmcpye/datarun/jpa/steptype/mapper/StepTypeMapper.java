package org.nmcpye.datarun.jpa.steptype.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.steptype.StepType;
import org.nmcpye.datarun.jpa.steptype.dto.StepTypeDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface StepTypeMapper extends BaseMapper<StepTypeDto, StepType> {
    @Override
    @Mappings({
        @Mapping(source = "flowType", target = "flowType.uid"),
        @Mapping(source = "dataTemplateUid", target = "dataTemplate.uid")
    })
    StepType toEntity(StepTypeDto stepTypeDto);

    @Override
    @Mappings({
        @Mapping(source = "flowType.uid", target = "flowType"),
        @Mapping(source = "dataTemplate.uid", target = "dataTemplateUid")
    })
    StepTypeDto toDto(StepType stepType);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
        @Mapping(source = "flowType", target = "flowType.uid"),
        @Mapping(source = "dataTemplateUid", target = "dataTemplate.uid")
    })
    StepType partialUpdate(StepTypeDto stepTypeDto, @MappingTarget StepType stepType);
}
