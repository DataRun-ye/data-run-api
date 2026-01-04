package org.nmcpye.datarun.jpa.assignment.mapper;

import org.nmcpye.datarun.jpa.assignment.dto.AssignmentMemberDto;
import org.nmcpye.datarun.jpa.assignment.AssignmentMember;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMemberMapper {

    public AssignmentMemberDto toDto(AssignmentMember entity) {
        if (entity == null) return null;
        return AssignmentMemberDto.builder()
            .id(entity.getId())
            .assignmentId(entity.getAssignmentId())
            .memberType(entity.getMemberType())
            .memberId(entity.getMemberId())
            .role(entity.getRole())
            .build();
    }

    public AssignmentMember toEntity(AssignmentMemberDto dto) {
        if (dto == null) return null;
        return AssignmentMember.builder()
            .id(dto.getId())
            .assignmentId(dto.getAssignmentId())
            .memberType(dto.getMemberType())
            .memberId(dto.getMemberId())
            .role(dto.getRole())
            .build();
    }
}
