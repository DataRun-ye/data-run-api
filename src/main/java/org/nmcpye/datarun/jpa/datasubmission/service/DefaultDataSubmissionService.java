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
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionKind;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionWriter;
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
 * The submission persistence layer behind {@code SubmissionCommandService}: it stores submissions and, in the same
 * transaction, records each change as a revision ({@link SubmissionRevisionWriter}) and announces it in the outbox.
 *
 * <p>A revision is written if and only if the submission's version moves: creation stores version 0, and every
 * flushed change of a stored submission raises {@code lock_version} by one (JPA's {@code @Version}). The versions
 * are read back after each flush, so whatever this layer persists is recorded, and an unchanged re-send, which
 * writes nothing, records nothing. Each revision gets exactly one outbox row, written after it: the outbox
 * announces revisions and nothing else.</p>
 */
@Service
@Primary
@Slf4j
public class DefaultDataSubmissionService
    extends DefaultJpaSoftDeleteService<DataSubmission>
    implements DataSubmissionService {
    private final ObjectMapper objectMapper;
    private final OutboxWritePort outboxRepo;
    private final SubmissionRevisionWriter revisions;

    public DefaultDataSubmissionService(
        DataSubmissionRepository repository,
        CacheManager cacheManager,
        UserAccessService userAccessService,
        ObjectMapper objectMapper, OutboxWritePort outboxRepo,
        SubmissionRevisionWriter revisions) {
        super(repository, cacheManager, userAccessService);
        this.objectMapper = objectMapper;
        this.outboxRepo = outboxRepo;
        this.revisions = revisions;
    }

    /** A stored submission's version and deleted state as loaded, before this write changed anything. */
    private record Stored(Long version, boolean deleted) {
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
        EntitySaveSummaryVM summary,
        RevisionContext revision) {
        Objects.requireNonNull(revision, "revision");
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        Set<String> incomingUids = entities.stream()
            .map(entity -> {
                if (entity.getUid() == null) {
                    throw new IllegalArgumentException("Entity in bulk operation must have a UID for upsert operation.");
                }
                return entity.getUid();
            })
            .collect(Collectors.toSet());

        List<DataSubmission> existingEntitiesFromDb = jpaAuditableObjectRepository.findAllByUidIn(new ArrayList<>(incomingUids));
        Map<String, DataSubmission> existingEntitiesMap = existingEntitiesFromDb.stream()
            .collect(Collectors.toMap(JpaSoftDeleteObject::getUid, Function.identity()));
        Map<DataSubmission, Stored> stored = new IdentityHashMap<>();
        existingEntitiesFromDb.forEach(existing ->
            stored.put(existing, new Stored(existing.getLockVersion(), Boolean.TRUE.equals(existing.getDeleted()))));

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
            summary.getCreated().addAll(persistedResults.stream().map(DataSubmission::getUid).toList());
        }
        if (!entitiesToUpdate.isEmpty()) {
            jpaAuditableObjectRepository.updateAllAndFlush(entitiesToUpdate);
        }
        if (!entitiesToDelete.isEmpty()) {
            jpaAuditableObjectRepository.updateAllAndFlush(entitiesToDelete);
        }

        persistedResults.forEach(created -> record(created, null, revision));
        recordChanges(entities, existingEntitiesMap, stored, revision);

        summary.getUpdated().addAll(normalResults.stream().map(DataSubmission::getUid).toList());
        summary.getUpdated().addAll(deleteResults.stream().map(DataSubmission::getUid).toList());

        List<DataSubmission> combinedResults = new ArrayList<>(persistedResults);
        combinedResults.addAll(normalResults);
        combinedResults.addAll(deleteResults);
        return combinedResults;
    }


    /**
     * Records a revision for every stored submission of this write whose version moved, in the order the write
     * received them: the flushes above raised it for each one they changed.
     */
    private void recordChanges(
        Collection<DataSubmission> incoming,
        Map<String, DataSubmission> existingByUid,
        Map<DataSubmission, Stored> stored,
        RevisionContext revision) {
        Set<DataSubmission> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (DataSubmission entity : incoming) {
            DataSubmission existing = existingByUid.get(entity.getUid());
            if (existing != null && seen.add(existing)) {
                Stored before = stored.get(existing);
                if (!Objects.equals(before.version(), existing.getLockVersion())) {
                    record(existing, before, revision);
                }
            }
        }
    }

    /**
     * Records one flushed change of a submission: its revision, then the outbox row that announces it, which names
     * the revision by the submission's id and version.
     *
     * @param before the submission as loaded, or null when this write created it
     */
    private void record(DataSubmission submission, Stored before, RevisionContext revision) {
        long version = submission.getLockVersion();
        boolean deleted = Boolean.TRUE.equals(submission.getDeleted());
        RevisionKind kind = before == null ? RevisionKind.CREATE
            : !before.deleted() && deleted ? RevisionKind.DELETE : RevisionKind.UPDATE;
        revisions.append(submission.getId(), version, before == null ? null : before.version(), kind, revision);
        outboxRepo.insert(List.of(outboxRow(submission, version, eventType(kind, deleted))));
    }

    /**
     * The event follows the state after the change: DELETE whenever the submission is deleted afterwards (a new
     * submission sent already deleted, a re-send onto a deleted one), so the ETL never projects a deleted record's
     * values as live; otherwise SAVE for a creation and UPDATE for a change.
     */
    private static String eventType(RevisionKind kind, boolean deletedAfter) {
        if (deletedAfter) {
            return "DELETE";
        }
        return kind == RevisionKind.CREATE ? "SAVE" : "UPDATE";
    }

    /**
     * The delete is flushed first, so that its revision and its DELETE event carry the version it gave. The submission
     * is the managed one the command loaded in this transaction, so the flush writes it.
     */
    @Transactional
    @Override
    public void softDelete(DataSubmission submission, RevisionContext revision) {
        Objects.requireNonNull(revision, "revision");
        Stored before = new Stored(submission.getLockVersion(), Boolean.TRUE.equals(submission.getDeleted()));
        submission.setDeleted(Boolean.TRUE);
        submission.setDeletedAt(Instant.now());
        jpaAuditableObjectRepository.flush();
        record(submission, before, revision);
    }

    /**
     * Not for submissions: a submission is deleted only through {@code SubmissionCommandService}, which names who
     * deleted it ({@link #softDelete(DataSubmission, RevisionContext)}). The generic delete paths that reach this
     * method have no submission route.
     */
    @Override
    public void softDelete(DataSubmission submission) {
        throw new UnsupportedOperationException(
            "A submission is deleted only through SubmissionCommandService, which records who deleted it");
    }

    /**
     * Not for submissions: the generic save would write a submission without a revision or an outbox row. A
     * submission is created or changed only through {@code SubmissionCommandService} ({@link #upsertAll}). No route
     * reaches it: since 6.5.5 the submission resource has no generic POST or PUT, which answer 405 (A2 W4-01, W5-01).
     */
    @Override
    public DataSubmission save(DataSubmission submission) {
        throw new UnsupportedOperationException(
            "A submission is written only through SubmissionCommandService, which records the change");
    }

    /** Not for submissions, as {@link #save}. */
    @Override
    public DataSubmission saveWithRelations(DataSubmission submission) {
        throw new UnsupportedOperationException(
            "A submission is written only through SubmissionCommandService, which records the change");
    }

    /** Not for submissions, as {@link #save}. */
    @Override
    public DataSubmission update(DataSubmission submission) {
        throw new UnsupportedOperationException(
            "A submission is written only through SubmissionCommandService, which records the change");
    }

    private OutboxWritePort.OutboxInsert outboxRow(DataSubmission s, long version, String eventType) {
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
            s.getSerialNumber(),
            version,
            eventType
        );
    }
}
