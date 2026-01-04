package org.nmcpye.datarun.jpa.assignment.mapper;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentPartyBindingDto;
import org.nmcpye.datarun.jpa.assignment.AssignmentPartyBinding;
import org.nmcpye.datarun.party.service.BindingValidationService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentPartyBindingMapper {
    protected final BindingValidationService validationService;

    public AssignmentPartyBindingDto toDto(AssignmentPartyBinding entity) {
        if (entity == null) return null;
        return AssignmentPartyBindingDto.builder()
            .id(entity.getId())
            .uid(entity.getUid())
            .name(entity.getName())
            .assignmentUid(entity.getAssignment() != null ? entity.getAssignment().getUid() : null)
            .vocabularyUid(entity.getVocabulary() != null ? entity.getVocabulary().getUid() : null)
            .partySetUid(entity.getPartySet() != null ? entity.getPartySet().getUid() : null)
            .principalType(entity.getPrincipalType())
            .principalUid(entity.getPrincipalId())
            .combineMode(entity.getCombineMode())
            .build();
    }

    public AssignmentPartyBinding toEntity(AssignmentPartyBindingDto dto) {
        if (dto == null) return null;
        return validationService.validatedBinding(dto);
    }
}
