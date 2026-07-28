package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.outbox.repository.OutboxWritePort;
import org.springframework.cache.CacheManager;
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
    private final ObjectMapper objectMapper;
    private final OutboxWritePort outboxRepo;

    public DefaultDataSubmissionService(
        DataSubmissionRepository repository,
        CacheManager cacheManager,
        UserAccessService userAccessService,
        ObjectMapper objectMapper, OutboxWritePort outboxRepo) {
        super(repository, cacheManager, userAccessService);
        this.objectMapper = objectMapper;
        this.outboxRepo = outboxRepo;
    }

    @Transactional
    @Override
    public DataSubmission upsert(
        DataSubmission entity,
        EntitySaveSummaryVM summary) {
        // Absolutely minimal logic here. All actual work is delegated.
        // upsertAll will validate for null entity or null UID within the list.
        List<DataSubmission> results = upsertAll(List.of(entity), summary);

        // This check is a safeguard for unexpected behavior from upsertAll,
        // rather than input validation.
        if (results.isEmpty()) {
            throw new IllegalStateException("UpsertAll returned an empty list when processing a single entity. This indicates an internal logic error.");
        }
        return results.get(0);
    }

    private boolean mutableFieldsMatch(DataSubmission existingEntity, DataSubmission incomingEntity) {
        return Objects.equals(existingEntity.getFormData(), incomingEntity.getFormData())
            && Objects.equals(existingEntity.getActivity(), incomingEntity.getActivity())
            && Objects.equals(existingEntity.getAssignment(), incomingEntity.getAssignment())
            && Objects.equals(existingEntity.getTeam(), incomingEntity.getTeam())
            && Objects.equals(existingEntity.getOrgUnit(), incomingEntity.getOrgUnit())
            && Objects.equals(existingEntity.getStatus(), incomingEntity.getStatus())
            && Objects.equals(existingEntity.getOrgUnitCode(), incomingEntity.getOrgUnitCode())
            && Objects.equals(existingEntity.getOrgUnitName(), incomingEntity.getOrgUnitName())
            && Objects.equals(existingEntity.getTeamCode(), incomingEntity.getTeamCode());
    }

    private void updateMutableFields(DataSubmission existingEntity, DataSubmission incomingEntity) {
        existingEntity.setFormData(incomingEntity.getFormData() == null
            ? null
            : incomingEntity.getFormData().deepCopy());
        existingEntity.setActivity(incomingEntity.getActivity());
        existingEntity.setAssignment(incomingEntity.getAssignment());
        existingEntity.setTeam(incomingEntity.getTeam());
        existingEntity.setOrgUnit(incomingEntity.getOrgUnit());
        existingEntity.setStatus(incomingEntity.getStatus());
        existingEntity.setOrgUnitCode(incomingEntity.getOrgUnitCode());
        existingEntity.setOrgUnitName(incomingEntity.getOrgUnitName());
        existingEntity.setTeamCode(incomingEntity.getTeamCode());
    }

    @Transactional
    @Override
    public List<DataSubmission> upsertAll(
        Collection<DataSubmission> entities,
        EntitySaveSummaryVM summary) {
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
        List<DataSubmission> entitiesToDelete = new ArrayList<>();
        List<DataSubmission> normalResults = new ArrayList<>();
        List<DataSubmission> deleteResults = new ArrayList<>();

        for (DataSubmission incomingEntity : entities) {
            DataSubmission existingEntity = existingEntitiesMap.get(incomingEntity.getUid());
            boolean isNew = (existingEntity == null);

            if (isNew) {
                if (incomingEntity.getId() == null) {
                    incomingEntity.setId(CodeGenerator.nextUlid());
                }
                entitiesToPersist.add(incomingEntity);
            } else if (Boolean.TRUE.equals(incomingEntity.getDeleted())) {
                deleteResults.add(existingEntity);
                if (!Boolean.TRUE.equals(existingEntity.getDeleted())) {
                    existingEntity.setDeleted(true);
                    existingEntity.setDeletedAt(Instant.now());
                    entitiesToDelete.add(existingEntity);
                }
            } else {
                normalResults.add(existingEntity);
                if (!mutableFieldsMatch(existingEntity, incomingEntity)) {
                    updateMutableFields(existingEntity, incomingEntity);
                    entitiesToUpdate.add(existingEntity);
                }
            }
        }

        List<DataSubmission> persistedResults = List.of();

        if (!entitiesToPersist.isEmpty()) {
            persistedResults = jpaAuditableObjectRepository.persistAllAndFlush(entitiesToPersist);
            final var outboxEvents = persistedResults.stream()
                .map(this::enqueueSubmissionsOutbox)
                .toList();
            outboxRepo.insertByEventType(outboxEvents, "SAVE");
            summary.getCreated().addAll(persistedResults.stream().map(DataSubmission::getUid).toList());
        }
        if (!entitiesToUpdate.isEmpty()) {
            List<DataSubmission> updatedResults = jpaAuditableObjectRepository.updateAllAndFlush(entitiesToUpdate);
            final var outboxEvents = updatedResults.stream()
                .map(this::enqueueSubmissionsOutbox)
                .toList();
            outboxRepo.insertByEventType(outboxEvents, "UPDATE");
        }

        if (!entitiesToDelete.isEmpty()) {
            List<DataSubmission> deletedResults = jpaAuditableObjectRepository.updateAllAndFlush(entitiesToDelete);
            final var outboxEvents = deletedResults.stream()
                .map(this::enqueueSubmissionsOutbox)
                .toList();
            outboxRepo.insertByEventType(outboxEvents, "DELETE");
        }

        summary.getUpdated().addAll(normalResults.stream().map(DataSubmission::getUid).toList());
        summary.getUpdated().addAll(deleteResults.stream().map(DataSubmission::getUid).toList());

        List<DataSubmission> combinedResults = new ArrayList<>(persistedResults);
        combinedResults.addAll(normalResults);
        combinedResults.addAll(deleteResults);
        return combinedResults;
    }


    @Transactional
    @Override
    public void softDelete(DataSubmission object) {
        var outbox = enqueueSubmissionsOutbox(object);
        outboxRepo.insertByEventType(List.of(outbox), "DELETE");
        super.softDelete(object);
    }

    private OutboxWritePort.OutboxInsert enqueueSubmissionsOutbox(DataSubmission s) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(s.getFormData());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return new OutboxWritePort.OutboxInsert(
            s.getId(),
            s.getUid(),
            s.getFormVersion(),
            payload,
            Instant.now(),
            s.getSerialNumber()
        );
    }
}
