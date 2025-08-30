package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionChangeType;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionSavedEvent;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEventStatus;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataSubmission}.
 */
@Service
@Primary
@Slf4j
public class DefaultDataSubmissionService
    extends DefaultJpaSoftDeleteService<DataSubmission>
    implements DataSubmissionService {
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    public DefaultDataSubmissionService(
        DataSubmissionRepository repository,
        CacheManager cacheManager,
        UserAccessService userAccessService,
        ApplicationEventPublisher eventPublisher,
        ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository) {
        super(repository, cacheManager, userAccessService);
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    @Override
    public DataSubmission upsert(DataSubmission entity, CurrentUserDetails user, EntitySaveSummaryVM summary) {
        // Absolutely minimal logic here. All actual work is delegated.
        // upsertAll will validate for null entity or null UID within the list.
        List<DataSubmission> results = upsertAll(List.of(entity), user, summary);

        // This check is a safeguard for unexpected behavior from upsertAll,
        // rather than input validation.
        if (results.isEmpty()) {
            throw new IllegalStateException("UpsertAll returned an empty list when processing a single entity. This indicates an internal logic error.");
        }
        return results.get(0);
    }


    private void afterPersist(DataSubmission result) {

    }

    private void afterUpdate(DataSubmission result) {
        eventPublisher.publishEvent(new SubmissionSavedEvent(result.getId(),
            SubmissionChangeType.UPDATE, result.getLockVersion()));
    }

    /**
     * Abstract method to be implemented by concrete services.
     * This method is responsible for copying mutable fields from the incoming entity
     * onto the existing entity during an update operation.
     *
     * @param existingEntity The entity fetched from the database (managed).
     * @param incomingEntity The entity data provided for the update (detached).
     */
    private void updateEntityFields(DataSubmission existingEntity, DataSubmission incomingEntity) {
        existingEntity.setFormData(incomingEntity.getFormData().deepCopy());
        existingEntity.setActivity(incomingEntity.getActivity());
        existingEntity.setAssignment(incomingEntity.getAssignment());
        existingEntity.setTeam(incomingEntity.getTeam());
        existingEntity.setOrgUnit(incomingEntity.getOrgUnit());
        existingEntity.setStatus(incomingEntity.getStatus());
        existingEntity.setOrgUnitCode(incomingEntity.getOrgUnitCode());
        existingEntity.setOrgUnitName(incomingEntity.getOrgUnitName());
        existingEntity.setTeamCode(incomingEntity.getTeamCode());
        existingEntity.setDeleted(incomingEntity.getDeleted());
        existingEntity.setDeletedAt(incomingEntity.getDeletedAt());
    }

    @Transactional
    @Override
    public List<DataSubmission> upsertAll(Collection<DataSubmission> entities,
                                          CurrentUserDetails user, EntitySaveSummaryVM summary) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        Set<String> incomingUids = entities.stream()
            .map(entity -> {
                if (entity.getUid() == null) {
                    summary.getFailed().put("NULL UID", "Entity in bulk operation must have a UID for upsert operation.");
                    throw new IllegalArgumentException("Entity in bulk operation must have a UID for upsert operation.");
                }
                return entity.getUid();
            })
            .collect(Collectors.toSet());

        List<DataSubmission> existingEntitiesFromDb = jpaAuditableObjectRepository.findAllByUidIn(new ArrayList<>(incomingUids));
        Map<String, DataSubmission> existingEntitiesMap = existingEntitiesFromDb.stream()
            .collect(Collectors.toMap(JpaSoftDeleteObject::getUid, Function.identity()));

        List<DataSubmission> entitiesToPersist = new ArrayList<>();
        List<DataSubmission> entitiesToUpdate = new ArrayList<>();

        for (DataSubmission incomingEntity : entities) {
            DataSubmission existingEntity = existingEntitiesMap.get(incomingEntity.getUid());
            boolean isNew = (existingEntity == null);

            beforeUpsertChecks(incomingEntity, isNew, user); // Apply pre-upsert checks for each entity

            if (isNew) {
                if (incomingEntity.getId() == null) {
                    incomingEntity.setId(CodeGenerator.nextUlid());
                }
                beforePersist(incomingEntity);
                entitiesToPersist.add(incomingEntity);
            } else {
                beforeUpdate(existingEntity, incomingEntity);
                updateEntityFields(existingEntity, incomingEntity);
                entitiesToUpdate.add(existingEntity);
            }
        }

        List<DataSubmission> persistedResults = List.of();
        List<DataSubmission> updatedResults = List.of();

        if (!entitiesToPersist.isEmpty()) {
            persistedResults = jpaAuditableObjectRepository.persistAllAndFlush(entitiesToPersist);
            final var outboxEvents = persistedResults.stream()
                .map(s -> createOutboxEvent(s, "submission.saved"))
                .toList();
            outboxEventRepository.persistAllAndFlush(outboxEvents);

            persistedResults.forEach(s -> eventPublisher.publishEvent(new SubmissionSavedEvent(s.getId(),
                SubmissionChangeType.CREATE, s.getLockVersion())));
            summary.getCreated().addAll(persistedResults.stream().map(DataSubmission::getUid).toList());
        }
        if (!entitiesToUpdate.isEmpty()) {
            updatedResults = jpaAuditableObjectRepository.updateAllAndFlush(entitiesToUpdate);
            final var outboxEvents = updatedResults.stream()
                .map(s -> createOutboxEvent(s, "submission.updated"))
                .toList();
            outboxEventRepository.persistAllAndFlush(outboxEvents);

            updatedResults.forEach(s -> eventPublisher.publishEvent(new SubmissionSavedEvent(s.getId(),
                SubmissionChangeType.UPDATE, s.getLockVersion()))); // Apply post-update hook for each
            summary.getUpdated().addAll(updatedResults.stream().map(DataSubmission::getUid).toList());
        }

        List<DataSubmission> combinedResults = new ArrayList<>(persistedResults);
        combinedResults.addAll(updatedResults);
        return combinedResults;
    }

    private void beforeUpdate(DataSubmission existingEntity, DataSubmission incomingEntity) {
    }

    private void beforePersist(DataSubmission incomingEntity) {
    }

    private void beforeUpsertChecks(DataSubmission incomingEntity, boolean isNew, CurrentUserDetails user) {
    }


    private OutboxEvent createOutboxEvent(DataSubmission saved, String eventType) {
        OutboxEvent evOk = new OutboxEvent();
        evOk.setAggregateType("DataSubmission");
        evOk.setAggregateId(saved.getId());
        evOk.setEventType(eventType);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("submissionId", saved.getId());
        payload.put("submissionVersion", saved.getLockVersion());
        evOk.setPayload(payload);
        evOk.setStatus(OutboxEventStatus.PENDING);
        evOk.setAttempts(0);
        evOk.setCreatedAt(Instant.now());
        evOk.setAvailableAt(Instant.now());
        return evOk;
    }
}
