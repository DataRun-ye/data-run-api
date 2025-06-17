package org.nmcpye.datarun.jpa.flowinstance.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.mongo.accessfilter.FormAccessService;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AssignmentWithAccessMapper
    implements BaseMapper<AssignmentWithAccessDto, FlowInstance> {

    @Autowired
    public FormAccessService formAccessService;

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "activity.id", source = "activity"),
        @Mapping(target = "orgUnit.id", source = "orgUnit"),
        @Mapping(target = "team.id", source = "team"),
        @Mapping(target = "status", source = "progressStatus"),
//        @Mapping(source = "accessibleForms.formUid", target = "forms"),
    })
    public abstract FlowInstance toEntity(AssignmentWithAccessDto dto);

    @Mappings({
        @Mapping(target = "activity", source = "activity.id"),
        @Mapping(source = "id", target = "id"),
        @Mapping(target = "orgUnit", source = "orgUnit.id"),
        @Mapping(target = "team", source = "team.id"),
        @Mapping(target = "progressStatus", source = "status", defaultValue = "PLANNED"),
        @Mapping(target = "accessibleForms",
            expression =
                "java(formAccessService.getUserForms(entity.getForms(), entity.getUid()))"),
    })
    public abstract AssignmentWithAccessDto toDto(FlowInstance entity);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract FlowInstance partialUpdate(@MappingTarget FlowInstance stepType, AssignmentWithAccessDto stepTypeDto);
}
