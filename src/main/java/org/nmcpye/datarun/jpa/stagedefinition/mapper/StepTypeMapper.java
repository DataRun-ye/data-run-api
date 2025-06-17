package org.nmcpye.datarun.jpa.stagedefinition.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;
import org.nmcpye.datarun.jpa.stagedefinition.dto.StepTypeDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface StepTypeMapper extends BaseMapper<StepTypeDto, StageDefinition> {

    @Override
    @Mapping(source = "flowTypeForceStepOrder", target = "flowType.forceStepOrder")
    @Mapping(source = "flowTypePlanningMode", target = "flowType.planningMode")
    @Mapping(source = "flowTypeId", target = "flowType.id")
    StageDefinition toEntity(StepTypeDto stepTypeDto);

    @InheritInverseConfiguration(name = "toEntity")
    @Override
    StepTypeDto toDto(StageDefinition stageDefinition);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    StageDefinition partialUpdate(@MappingTarget StageDefinition stageDefinition, StepTypeDto stepTypeDto);
}
