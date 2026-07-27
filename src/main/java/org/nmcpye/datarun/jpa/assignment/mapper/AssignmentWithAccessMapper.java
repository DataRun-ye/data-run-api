package org.nmcpye.datarun.jpa.assignment.mapper;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentWithAccessMapper {

    private final AssignmentFormAccessService formAccessService;

    public AssignmentWithAccessDto toDto(
        Assignment assignment,
        CurrentUserDetails user
    ) {
        AssignmentWithAccessDto dto = new AssignmentWithAccessDto();
        dto.setId(assignment.getUid());
        dto.setCode(assignment.getCode());
        dto.setActivity(
            assignment.getActivity() == null
                ? null
                : assignment.getActivity().getUid());
        dto.setOrgUnit(
            assignment.getOrgUnit() == null
                ? null
                : assignment.getOrgUnit().getUid());
        dto.setTeam(
            assignment.getTeam() == null
                ? null
                : assignment.getTeam().getUid());
        dto.setProgressStatus(
            assignment.getStatus() == null
                ? FlowStatus.PLANNED
                : assignment.getStatus());
        dto.setAccessibleForms(
            formAccessService.getAccessibleForms(assignment, user));
        return dto;
    }
}
