package org.nmcpye.datarun.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.assignment.Assignment;
import org.nmcpye.datarun.mapper.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.security.useraccess.dataform.FormAccessService;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AssignmentWithAccessMapper
    implements BaseMapper<AssignmentWithAccessDto, Assignment> {

    @Autowired
    public FormAccessService formAccessService;

    @Mappings({
        @Mapping(source = "activity", target = "activity.uid"),
        @Mapping(source = "assignment", target = "uid"),
        @Mapping(source = "orgUnit", target = "orgUnit.uid"),
        @Mapping(source = "team", target = "team.uid"),
        @Mapping(source = "progressStatus", target = "status"),
//        @Mapping(source = "accessibleForms.formUid", target = "forms"),
    })
    public abstract Assignment toEntity(AssignmentWithAccessDto dto);

    @Mappings({
        @Mapping(target = "activity", source = "activity.uid"),
        @Mapping(target = "assignment", source = "uid"),
        @Mapping(target = "orgUnit", source = "orgUnit.uid"),
        @Mapping(target = "team", source = "team.uid"),
        @Mapping(target = "progressStatus", source = "status", defaultValue = "PLANNED"),
        @Mapping(target = "accessibleForms",
            expression =
                "java(formAccessService.getUserForms(entity.getForms(), entity.getUid()))"),
    })
    public abstract AssignmentWithAccessDto toDto(Assignment entity);


}
