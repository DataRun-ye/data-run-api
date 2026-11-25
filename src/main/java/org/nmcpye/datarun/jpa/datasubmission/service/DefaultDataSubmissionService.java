package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
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

    /**
     * A new submission is persisted at version 0. A stored one, which the caller holds under the submission lock, is
     * updated in place: a first {@code deleted=true} marks it deleted and keeps its values; any other copy whose
     * persisted fields differ replaces them; anything else writes nothing. The flush raises the version of a change,
     * and that change is recorded.
     */
    @Transactional
    @Override
    public void upsert(
        DataSubmission incoming,
        DataSubmission stored,
        EntitySaveSummaryVM summary,
        RevisionContext revision) {
        Objects.requireNonNull(revision, "revision");
        if (incoming.getUid() == null) {
            throw new IllegalArgumentException("A submission must have a uid to be saved");
        }
        if (stored != null && !incoming.getUid().equals(stored.getUid())) {
            throw new IllegalArgumentException("The stored submission is not the incoming one");
        }

        if (stored == null) {
            if (incoming.getId() == null) {
                incoming.setId(CodeGenerator.nextUlid());
            }
            List<DataSubmission> persisted = jpaAuditableObjectRepository.persistAllAndFlush(List.of(incoming));
            summary.getCreated().addAll(persisted.stream().map(DataSubmission::getUid).toList());
            persisted.forEach(created -> record(created, null, revision));
            return;
        }

        Stored before = new Stored(stored.getLockVersion(), Boolean.TRUE.equals(stored.getDeleted()));
        if (Boolean.TRUE.equals(incoming.getDeleted())) {
            if (!before.deleted()) {
                stored.setDeleted(true);
                stored.setDeletedAt(Instant.now());
                jpaAuditableObjectRepository.updateAllAndFlush(List.of(stored));
            }
        } else if (!mutableFieldsMatch(stored, incoming)) {
            updateMutableFields(stored, incoming);
            jpaAuditableObjectRepository.updateAllAndFlush(List.of(stored));
        }
        if (!Objects.equals(before.version(), stored.getLockVersion())) {
            record(stored, before, revision);
        }
        summary.getUpdated().add(stored.getUid());
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
     * submission is created or changed only through {@code SubmissionCommandService} ({@link #upsert}). No route
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
