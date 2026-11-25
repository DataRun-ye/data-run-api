package org.nmcpye.datarun.etl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.etl.dto.FlattenSubmission;
import org.nmcpye.datarun.etl.mapper.DataSubmissionMapper;
import org.nmcpye.datarun.etl.model.SubmissionContext;
import org.nmcpye.datarun.etl.model.TemplateContext;
import org.nmcpye.datarun.etl.repository.EventEntityJdbcRepository;
import org.nmcpye.datarun.etl.repository.TallCanonicalJdbcRepository;
import org.nmcpye.datarun.etl.service.EventProcessorService;
import org.nmcpye.datarun.etl.service.SubmissionNotFoundException;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.nmcpye.datarun.jpa.datatemplate.repository.CanonicalElementRepository;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessorServiceImpl implements EventProcessorService {

    private final DataSubmissionRepository submissionRepository;
    private final TransformServiceV2 transformService;
    private final TallCanonicalJdbcRepository tallRepo;
    private final DataSubmissionMapper dataSubmissionMapper;
    private final CanonicalElementRepository canonicalElementRepository;
    private final EventEntityJdbcRepository eventJdbcRepository;

    /**
     * Projects one claimed outbox row into tall and events, inside the caller's transaction. Any failure propagates;
     * the caller records the row's outcome.
     *
     * @param outbox the claimed outbox DTO (event type, submission uid, and the ingest id its claim assigned)
     */
    @Override
    public void processEvent(OutboxDto outbox) {
        if (outbox == null) return;
        final var ingestId = outbox.getIngestId();

        final Long outboxId = outbox.getOutboxId();
        final String eventType = outbox.getEventType() == null ? "SAVE" : outbox.getEventType().toUpperCase();
        if ("DELETE".equals(eventType)) {
            eventJdbcRepository.markAllAsDeletedForSubmission(outbox.getSubmissionUid());
            log.debug("Deleted events rows for submission_uid={} (outboxId={})",
                outbox.getSubmissionUid(), outboxId);
            return;
        }

        final var submission = submissionRepository.findByUid(outbox.getSubmissionUid())
            .orElseThrow(SubmissionNotFoundException::new);
        JsonNode payload = submission.getFormData();

        final SubmissionContext submissionContext = dataSubmissionMapper.toDto(submission)
            .ingestId(ingestId)
            .outboxId(outboxId);

        // 3) load CE metadata for templateVersionUid and index by canonical_element_id
        var elements = canonicalElementRepository
            .findByTemplateUid(submission.getForm());
        var allCEsMap = elements.stream()
            .collect(Collectors.toMap(CanonicalElement::getId, Function.identity()));
        var repeatEsMap = elements.stream()
            .filter(CanonicalElement::isRepeatCE)
            .collect(Collectors.toMap(CanonicalElement::getId, Function.identity()));

        TemplateContext templateContext = TemplateContext.builder()
            .allCanonicalElementsMap(allCEsMap)
            .repeatCanonicalElementsMap(repeatEsMap)
            .build();

        // SAVE / UPDATE / REPLACE / BACKFILL -> mapping + upsert
        // JSON Traverse transform
        FlattenSubmission flattenSubmission = transformService.transform(payload,
            false, submissionContext.submissionUid(), templateContext);


        if (flattenSubmission == null) {
            // nothing to persist -> still a successful processing
            return;
        }

        // idempotent upsert: unique constraint on instance_key + canonical_element_uid prevents duplicates
        log.debug("About to upsert {} events and {} tall rows for submissionUid={}",
            flattenSubmission.eventRows().size(), flattenSubmission.tallCanonicalRows().size(), submission.getUid());
        tallRepo.upsertBatch(submissionContext, flattenSubmission.tallCanonicalRows());
        eventJdbcRepository.patchUpsertEvents(submissionContext, flattenSubmission.eventRows());
    }

}
