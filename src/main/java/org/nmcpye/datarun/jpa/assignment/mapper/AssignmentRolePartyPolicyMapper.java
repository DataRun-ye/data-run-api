package org.nmcpye.datarun.jpa.assignment.mapper;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentRolePartyPolicyDto;
import org.nmcpye.datarun.jpa.assignment.AssignmentRolePartyPolicy;
import org.nmcpye.datarun.party.service.BindingValidationService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentRolePartyPolicyMapper {
    protected final BindingValidationService validationService;

    public AssignmentRolePartyPolicyDto toDto(AssignmentRolePartyPolicy entity) {
        if (entity == null)
            return null;
        return AssignmentRolePartyPolicyDto.builder()
                .id(entity.getId())
                .uid(entity.getUid())
                .name(entity.getName())
                .assignmentUid(entity.getAssignment() != null ? entity.getAssignment().getUid() : null)
                .vocabularyUid(entity.getVocabulary() != null ? entity.getVocabulary().getUid() : null)
                .partySetUid(entity.getPartySet() != null ? entity.getPartySet().getUid() : null)
                .role(entity.getRole())
                .combineMode(entity.getCombineMode())
                .build();
    }

    public AssignmentRolePartyPolicy toEntity(AssignmentRolePartyPolicyDto dto) {
        if (dto == null)
            return null;
        return validationService.validatedBinding(dto);
    }
}
