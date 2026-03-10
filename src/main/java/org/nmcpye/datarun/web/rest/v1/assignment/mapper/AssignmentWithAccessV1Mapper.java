package org.nmcpye.datarun.web.rest.v1.assignment.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.datatemplateprocessor.FormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.web.rest.v1.assignment.dto.AssignmentFormV1Dto;
import org.nmcpye.datarun.web.rest.v1.assignment.dto.AssignmentWithAccessV1Dto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, uses = {
        FormAccessService.class })
public interface AssignmentWithAccessV1Mapper {

    @Mappings({
            @Mapping(target = "activity", source = "activity.uid"),
            @Mapping(target = "orgUnit", source = "orgUnit.uid"),
            @Mapping(target = "team", source = "team.uid"),
            @Mapping(target = "progressStatus", source = "status", defaultValue = "PLANNED"),
            @Mapping(target = "accessibleForms", source = "assignment", qualifiedByName = "assignmentUserForms"),
    })
    AssignmentWithAccessV1Dto toDto(Assignment assignment);

    default AssignmentFormV1Dto toV1Dto(org.nmcpye.datarun.jpa.assignment.dto.AssignmentFormDto dto) {
        if (dto == null) {
            return null;
        }
        return AssignmentFormV1Dto.builder()
                .assignment(dto.getAssignment())
                .form(dto.getForm())
                .canAddSubmissions(dto.isCanAddSubmissions())
                .canEditSubmissions(dto.isCanEditSubmissions())
                .canDeleteSubmissions(dto.isCanDeleteSubmissions())
                .build();
    }
}
