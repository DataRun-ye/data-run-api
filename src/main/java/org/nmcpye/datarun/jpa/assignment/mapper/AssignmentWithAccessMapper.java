package org.nmcpye.datarun.jpa.assignment.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.mongo.accessfilter.FormAccessService;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AssignmentWithAccessMapper
    implements BaseMapper<AssignmentWithAccessDto, Assignment> {

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
    public abstract Assignment toEntity(AssignmentWithAccessDto dto);

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
    public abstract AssignmentWithAccessDto toDto(Assignment entity);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract Assignment partialUpdate(@MappingTarget Assignment stepType, AssignmentWithAccessDto stepTypeDto);
}
