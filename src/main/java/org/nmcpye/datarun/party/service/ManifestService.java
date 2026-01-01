package org.nmcpye.datarun.party.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.party.dto.AssignmentManifestDto;
import org.nmcpye.datarun.party.dto.AssignmentStatus;
import org.nmcpye.datarun.party.entities.AssignmentPartyBinding;
import org.nmcpye.datarun.party.repository.AssignmentMemberRepository;
import org.nmcpye.datarun.party.repository.AssignmentPartyBindingRepository;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManifestService {

    private final AssignmentRepository assignmentRepo;
    private final AssignmentMemberRepository memberRepo;
    /**
     * vocabRepo
     */
    private final DataTemplateRepository templateRepository;
    private final AssignmentPartyBindingRepository bindingRepo;

    // In a real app, you'd inject a "UserContext" to get the current user's ID
    public List<AssignmentManifestDto> buildManifest(String userUid, List<String> teamUids) {

        // 1. Find active Assignment IDs for this user/team
        List<String> assignmentIds = List.of(); // not implemented `memberRepo.findActiveAssignmentIds(userUid, teamUids)`;

        if (assignmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Fetch the Assignment Entities
        List<Assignment> assignments = assignmentRepo.findAllById(assignmentIds);

        // 3. Build DTOs
        return assignments.stream().map(assign -> {

            // Fetch linked Vocabularies (Form Templates)
            List<String> vocabUids = assignmentRepo.findById(assign.getId())
                .map(Assignment::getForms)
                .stream()
                .flatMap(Collection::stream).toList();

            // Fetch Bindings (Rules)
            // We expose these so the client can cache requests by 'partySetUid'
            List<AssignmentPartyBinding> bindings = bindingRepo.findByAssignmentId(assign.getId());

            return AssignmentManifestDto.builder()
                .assignmentUid(assign.getUid())
                .label(assign.getOrgUnit().getName())
                .status(AssignmentStatus.getAssignmentStatus(assign.getStatus()))
                .templateUids(vocabUids)
                .bindings(mapBindings(bindings))
                .build();

        }).toList();
    }

    private List<AssignmentManifestDto.BindingDto> mapBindings(List<AssignmentPartyBinding> source) {
        return source.stream().map(b -> AssignmentManifestDto.BindingDto.builder()
                .roleName(b.getName())
                .templateUid(templateRepository.findById(b.getVocabularyId())
                    .orElseThrow(() -> new NotFoundException("no template with id" + b.getVocabularyId()))
                    .getUid())
                .partySetId(b.getPartySetId()) // Exposing ID allows client to use it as a cache key
                .build())
            .toList();
    }
}
