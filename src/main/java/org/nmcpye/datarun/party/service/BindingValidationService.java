package org.nmcpye.datarun.party.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.AssignmentPartyBinding;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentPartyBindingDto;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.PrincipalResolveService;
import org.nmcpye.datarun.jpa.assignment.service.PrincipalResolveService.ResolvedPrincipal;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.party.entities.PartySet;
import org.nmcpye.datarun.party.exceptions.InvalidBindingException;
import org.nmcpye.datarun.party.exceptions.NotFoundObjectException;
import org.nmcpye.datarun.party.repository.PartySetRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BindingValidationService {

    private final PrincipalResolveService principalResolveService;
    private final PartySetRepository partySetRepository;
    private final AssignmentRepository assignmentRepository;
    private final DataTemplateRepository dataTemplateRepository;

    public AssignmentPartyBinding validatedBinding(AssignmentPartyBindingDto dto) {
        if (dto.getPartySetUid() == null) {
            throw new InvalidBindingException("PartySet ID cannot be null.");
        }
        if (dto.getAssignmentUid() == null || dto.getAssignmentUid().isBlank()) {
            throw new InvalidBindingException("Assignment UID cannot be null.");
        }

        final var principal = validatedBindingPrincipal(dto);
        final var binding = AssignmentPartyBinding.builder()
            .assignment(validatedBindingAssignment(dto).persisted())
            .partySet(validatedBindingPartySet(dto).persisted())
            .vocabulary(validatedBindingVocabulary(dto).persisted())
            .principalId(principal != null ? principal.getPrincipalId() : null)
            .principalType(principal != null ? principal.getPrincipalType() : null)
            .combineMode(dto.getCombineMode())
            .name(dto.getName())
            .build();

        return binding.toBuilder()
            .uid(dto.getUid() != null ? dto.getUid() : CodeGenerator.generateUid())
            .id(dto.getId() != null ? dto.getId() : UUID.randomUUID())
            .build();
    }

    public Assignment validatedBindingAssignment(AssignmentPartyBindingDto dto) {
        return assignmentRepository.findByUid(dto.getAssignmentUid())
            .orElseThrow(() ->
                new NotFoundObjectException("Assignment with UID " + dto.getAssignmentUid() + " does not exist."));
    }

    public PartySet validatedBindingPartySet(AssignmentPartyBindingDto dto) {
        return partySetRepository.findByUid(dto.getAssignmentUid())
            .orElseThrow(() ->
                new NotFoundObjectException("PartySet with UID " + dto.getPartySetUid() + " does not exist."));
    }

    public DataTemplate validatedBindingVocabulary(AssignmentPartyBindingDto dto) {
        return dataTemplateRepository.findByUid(dto.getAssignmentUid()).orElse(null);
    }

    public ResolvedPrincipal validatedBindingPrincipal(AssignmentPartyBindingDto dto) {
        return principalResolveService.resolvePrincipal(dto.getPrincipalType(), dto.getPrincipalUid()).orElse(null);
    }
}
