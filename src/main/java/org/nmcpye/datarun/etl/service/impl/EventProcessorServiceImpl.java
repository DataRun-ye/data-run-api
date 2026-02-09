package org.nmcpye.datarun.etl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.mapper.DataSubmissionMapper;
import org.nmcpye.datarun.etl.model.SubmissionContext;
import org.nmcpye.datarun.etl.model.TallCanonicalRow;
import org.nmcpye.datarun.etl.model.TemplateContext;
import org.nmcpye.datarun.etl.repository.EventEntityJdbcRepository;
import org.nmcpye.datarun.etl.repository.TallCanonicalJdbcRepository;
import org.nmcpye.datarun.etl.service.EventProcessorService;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.nmcpye.datarun.jpa.datatemplate.repository.CanonicalElementRepository;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.service.OutboxProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Event processor that applies the semantics for outbox event types (SAVE/UPDATE/DELETE/BACKFILL).
 * <p>
 * Notes:
 * - This class assumes the caller (Orchestrator) executes processEvent(...) within a per-outbox transaction.
 * - On failures we persist a failure record in a separate REQUIRES_NEW transaction so the failure audit
 * is durable even if the outer TX rolls back.
 * - Idempotency is provided by the TallCanonicalJdbcRepository upsert (unique instance_key + canonical_element_uid).
 */
@Service
@RequiredArgsConstructor
public class EventProcessorServiceImpl implements EventProcessorService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessorServiceImpl.class);

    private final DataSubmissionRepository submissionRepository;
    private final TransformServiceV2 transformServiceRobust;
    private final TallCanonicalJdbcRepository tallRepo;
    private final OutboxProcessingService outboxProcessingService;
    private final PlatformTransactionManager txManager;
    private final DataSubmissionMapper dataSubmissionMapper;
    private final CanonicalElementRepository canonicalElementRepository;
    private final EventEntityJdbcRepository eventJdbcRepository;

    /**
     * Process a single outbox event.
     * Keep per-outbox transactional boundary opinionated: this class should rely on caller to run processEvent
     * inside a transaction; however `recordFailureRequiresNewTx` must be REQUIRES_NEW to persist
     * failures when outer TX rolls back.
     *
     * @param etlRunId the current etl run id (used for bookkeeping)
     * @param outbox   the claimed outbox DTO (contains event type, repeat ids, submission uid)
     * @param ingestId the run-level ingest id assigned during claim
     * @throws Exception rethrows any transform/upsert exceptions so orchestrator can handle higher-level flow
     */
    @Override
    public void processEvent(UUID etlRunId, OutboxDto outbox, UUID ingestId) throws Exception {
        if (outbox == null) return;

        final Long outboxId = outbox.getOutboxId();
        final String eventType = outbox.getEventType() == null ? "SAVE" : outbox.getEventType().toUpperCase();
        try {
            if ("DELETE".equals(eventType)) {
                eventJdbcRepository.markAllAsDeletedForSubmission(outbox.getSubmissionUid());
                log.debug("Deleted events rows for submission_uid={} (outboxId={})",
                    outbox.getSubmissionUid(), outboxId);

                // record success (recordSuccess will update outbox.status and etl run counters)
                outboxProcessingService.recordSuccess(etlRunId, outbox);
                // outboxProcessingService.recordSuccess handles markOutboxSuccess and etlRunService.incrementSuccess
                return;
            }

            final var submissionOptional = submissionRepository.findByUid(outbox.getSubmissionUid());

            if(submissionOptional.isEmpty()) return;

            final var submission = submissionOptional.get();
            JsonNode payload = submission.getFormData();

            final SubmissionContext submissionContext = dataSubmissionMapper.toDto(submission);

            // 3) load CE metadata for templateVersionUid and index by canonical_element_id
            var elements = canonicalElementRepository.findByTemplateUid(submissionContext.getTemplateUid());
            var allCEsMap = elements.stream()
                .collect(Collectors.toMap(CanonicalElement::getId, Function.identity()));
            var repeatEsMap = elements.stream()
                .filter(CanonicalElement::isRepeatCE)
                .collect(Collectors.toMap(CanonicalElement::getId, Function.identity()));

            TemplateContext templateContext = TemplateContext.builder()
                .templateUid(submissionContext.getTemplateUid())
                .allCanonicalElementsMap(allCEsMap)
                .repeatCanonicalElementsMap(repeatEsMap)
                .build();

            // SAVE / UPDATE / REPLACE / BACKFILL -> mapping + upsert
            // JSON Traverse transform
            List<TallCanonicalRow> robustRows = transformServiceRobust.transform(payload,
                false, submissionContext, templateContext);

            log.debug("Transform produced {} rows for outboxId={}, submission_uid={}", robustRows == null ? 0 :
                robustRows.size(), outbox.getOutboxId(), outbox.getSubmissionUid());

            if (robustRows == null || robustRows.isEmpty()) {
                // nothing to persist -> still a successful processing
                outboxProcessingService.recordSuccess(etlRunId, outbox);
                return;
            }

            // ensure provenance fields filled before persisting
            for (TallCanonicalRow r : robustRows) {
                r.setIngestId(ingestId);
                r.setOutboxId(outboxId);
                r.setSubmissionSerialNumber(submissionContext.getSubmissionSerial());
                r.setSubmissionId(submissionContext.getSubmissionId());
                r.setSubmissionUid(submissionContext.getSubmissionUid());
                r.setSubmissionCreationTime(submissionContext.getSubmissionCreationTime());
                r.setSubmissionStartTime(submissionContext.getStartTime());
                r.setTemplateUid(submissionContext.getTemplateUid());
                r.setTemplateVersionUid(submissionContext.getTemplateVersionUid());
                r.setActivity(submissionContext.getActivityUid());
                r.setAssignment(submissionContext.getAssignmentUid());
                r.setOrgUnit(submissionContext.getOrgUnitUid());
                r.setCreatedBy(submissionContext.getCreatedBy());
                r.setLastModifiedBy(submissionContext.getLastModifiedBy());
                r.setTeam(submissionContext.getTeamUid());
                r.setIsDeleted(Boolean.FALSE);
            }

            // idempotent upsert: unique constraint on instance_key + canonical_element_uid prevents duplicates
            log.debug("About to upsert {} tall rows for submissionUid={}", robustRows.size(), submissionContext.getSubmissionUid());
            tallRepo.upsertBatch(robustRows);
            log.debug("tallRepo.upsertBatch affected={}", 0);
            // on success, record success (updates outbox and run counters)
            outboxProcessingService.recordSuccess(etlRunId, outbox);
        } catch (Exception ex) {
            // on any transient/DB/transform error, persist failure in a separate REQUIRES_NEW TX so it survives outer rollback
            String shortError = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            if (shortError.length() > 1000) shortError = shortError.substring(0, 1000) + "...(truncated)";

            Instant nextAttemptAt = Instant.now().plusSeconds(60); // simple backoff; orchestrator can later use smarter logic

            // recordFailure in a new transaction so failure record and outbox retry scheduling are durable
            recordFailureRequiresNewTx(etlRunId, outbox, shortError, nextAttemptAt);

            // rethrow to let orchestrator perform outer rollback / higher-level handling
            throw ex;
        }
    }

    /**
     * Persist failure details in a new, independent transaction so the failure audit and retry scheduling
     * are durable even if the calling transaction rolls back.
     * <p>
     * We use TransactionTemplate with PROPAGATION_REQUIRES_NEW to suspend the calling tx and start a fresh one.
     */
    public void recordFailureRequiresNewTx(UUID etlRunId, OutboxDto outbox, String shortError, Instant nextAttemptAt) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionTemplate txTemplate = new TransactionTemplate(txManager, def);
        txTemplate.execute(status -> {
            // delegate to existing OutboxProcessingService which inserts a failure record and updates outbox attempt info
            outboxProcessingService.recordFailure(etlRunId, outbox, shortError, nextAttemptAt);
            // note: outboxProcessingService.recordFailure will call OutboxJdbcRepository.markOutboxFailure and EtlRunService.incrementFailure
            return null;
        });
    }

}
