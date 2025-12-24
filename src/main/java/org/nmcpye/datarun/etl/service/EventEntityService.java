package org.nmcpye.datarun.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.EventDto;
import org.nmcpye.datarun.etl.repository.EventEntityJdbcRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventEntityService {
    private final EventEntityJdbcRepository eventRepo;

    /**
     * Upsert an event row for instanceKey. If eventUid is null we generate one;
     * repository upsert keeps original event_uid on conflict to preserve identity.
     */
    public void upsert(EventDto eventDto) {

//        String uid = eventUid != null ? eventUid : CodeGenerator.nextUlid();
        eventRepo.upsertEventEntity(eventDto);

//            .eventType(eventType)
//            .submissionUid(submissionUid)
//            .submissionId(submissionId)
//            .assignmentUid(assignmentUid)
//            .activityUid(activityUid)
//            .orgUnitUid(orgUnitUid)
//            .teamUid(teamUid)
//            .templateUid(templateUid)
//            .submissionCreationTime(submissionCreationTime)
//            .startTime(startTime)
//            .lastSeen(lastSeen)
//            .anchorCeId(anchorCeId)
//            .anchorRefUid(anchorRefUid)
//            .anchorValueText(anchorValueText)
//            .anchorValueRefType(anchorValueRefType)
//            .anchorConfidence(anchorConfidence)
//            .anchorResolvedAt(anchorResolvedAt)
//            .build());
    }

    public Optional<EventDto> findByInstanceKey(String instanceKey) {
        return eventRepo.findByInstanceKey(instanceKey);
    }
}
