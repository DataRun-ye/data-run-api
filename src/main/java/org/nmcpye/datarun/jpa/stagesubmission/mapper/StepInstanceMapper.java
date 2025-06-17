package org.nmcpye.datarun.jpa.stagesubmission.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;
import org.nmcpye.datarun.jpa.stagesubmission.dto.StepInstanceDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface StepInstanceMapper extends BaseMapper<StepInstanceDto, StageInstance> {
    @Override
    @Mapping(source = "stepTypeId", target = "stepType.id")
    StageInstance toEntity(StepInstanceDto stepInstanceDto);

    @Override
    @Mapping(source = "getStageDefinition.id", target = "stepTypeId")
    StepInstanceDto toDto(StageInstance stageInstance);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "stepTypeId", target = "stepType.id")
    StageInstance partialUpdate(StepInstanceDto stepInstanceDto, @MappingTarget StageInstance stageInstance);
}
