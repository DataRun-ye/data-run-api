package org.nmcpye.datarun.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.etl.dto.EventDto;
import org.nmcpye.datarun.etl.repository.EventEntityJdbcRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventEntityService {
    private final EventEntityJdbcRepository eventRepo;

    /**
     * Upsert an event row for instanceKey. If eventUid is null we generate one;
     * repository upsert keeps original event_uid on conflict to preserve identity.
     */
    public void upsert(String eventUid, String instanceKey, String eventType,
                       String submissionUid, String submissionId,
                       String assignmentUid, String activityUid,
                       String orgUnitUid, String teamUid, String templateUid,
                       Instant submissionCreationTime, Instant startTime, Instant lastSeen,
                       String anchorCeId, String anchorRefUid, String anchorValueText,
                       BigDecimal anchorConfidence, Instant anchorResolvedAt) {

        String uid = eventUid != null ? eventUid : CodeGenerator.nextUlid();
        eventRepo.upsertEventEntity(EventDto.builder()
            .eventUid(uid)
            .instanceKey(instanceKey)
            .eventType(eventType)
            .submissionUid(submissionUid)
            .submissionId(submissionId)
            .assignmentUid(assignmentUid)
            .activityUid(activityUid)
            .orgUnitUid(orgUnitUid)
            .teamUid(teamUid)
            .templateUid(templateUid)
            .submissionCreationTime(submissionCreationTime)
            .startTime(startTime)
            .lastSeen(lastSeen)
            .anchorCeId(anchorCeId)
            .anchorRefUid(anchorRefUid)
            .anchorValueText(anchorValueText)
            .anchorConfidence(anchorConfidence)
            .anchorResolvedAt(anchorResolvedAt)
            .build());
    }

    public Optional<EventDto> findByInstanceKey(String instanceKey) {
        return eventRepo.findByInstanceKey(instanceKey);
    }
}
