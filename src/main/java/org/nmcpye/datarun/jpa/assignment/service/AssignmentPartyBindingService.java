package org.nmcpye.datarun.jpa.assignment.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.assignment.AssignmentPartyBinding;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentPartyBindingDto;
import org.nmcpye.datarun.jpa.assignment.mapper.AssignmentPartyBindingMapper;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentPartyBindingRepository;
import org.nmcpye.datarun.jpa.datasubmission.events.EventChangeType;
import org.nmcpye.datarun.party.events.AssignmentBindingChangedEvent;
import org.nmcpye.datarun.utils.UuidUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignmentPartyBindingService {

    private final AssignmentPartyBindingRepository bindingRepository;
    private final AssignmentPartyBindingMapper bindingMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AssignmentPartyBindingDto save(AssignmentPartyBindingDto dto) {
        AssignmentPartyBinding entity = bindingMapper.toEntity(dto);
        // Add logic for generating UID if not present
        entity = bindingRepository.persist(entity);

        // **IMPORTANT**: Publish an event so permissions can be recalculated
        eventPublisher.publishEvent(new AssignmentBindingChangedEvent(entity, EventChangeType.CREATE));

        return bindingMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<AssignmentPartyBindingDto> findByAssignmentId(String assignmentId) {
        return bindingRepository.findByAssignmentIdOrAssignmentUid(assignmentId, assignmentId).stream()
            .map(bindingMapper::toDto)
            .collect(Collectors.toList());
    }

    public void delete(String id) {
        (UuidUtils.isUuid(id) ? bindingRepository.findById(UuidUtils.toUuid(id)) : bindingRepository.findByUid(id))
            .ifPresent(binding -> {
                bindingRepository.delete(binding);
                // **IMPORTANT**: Publish an event for deletion as well
                eventPublisher.publishEvent(new AssignmentBindingChangedEvent(binding, EventChangeType.DELETE));
            });
    }
}
