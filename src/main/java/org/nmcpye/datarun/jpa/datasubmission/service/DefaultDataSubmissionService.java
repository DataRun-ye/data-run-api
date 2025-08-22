package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.repository.CreateAccessDeniedException;
import org.nmcpye.datarun.common.repository.UpdateAccessDeniedException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.SubmissionDataProcessor;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionChangeType;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionSavedEvent;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEventStatus;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.nmcpye.datarun.mongo.accessfilter.FormAccessService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Service Implementation for managing {@link DataSubmission}.
 */
@Service
@Primary
@Slf4j
public class DefaultDataSubmissionService
    extends DefaultJpaSoftDeleteService<DataSubmission>
    implements DataSubmissionService {
    private final SubmissionDataProcessor submissionDataProcessor;
    private final FormAccessService formAccessService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private final OutboxEventRepository outboxEventRepository;

    public DefaultDataSubmissionService(
        DataSubmissionRepository repository,
        CacheManager cacheManager,
        UserAccessService userAccessService,
        SubmissionDataProcessor submissionDataProcessor,
        ApplicationEventPublisher eventPublisher,
        FormAccessService formAccessService,
        ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository) {
        super(repository, cacheManager, userAccessService);
        this.submissionDataProcessor = submissionDataProcessor;
        this.eventPublisher = eventPublisher;
        this.formAccessService = formAccessService;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public DataSubmission trySaveOrUpdate(DataSubmission incoming, CurrentUserDetails user) {
        submissionDataProcessor.processIncomingSubmission(incoming, user);
        Optional<DataSubmission> exisitingOptional = findByIdOrUid(incoming);
        SubmissionChangeType changeType;
        DataSubmission submission;
        if (exisitingOptional.isPresent()) {
            final var existing = exisitingOptional.get();
            throwIfChangedTemplateVersion(incoming, existing);
            if (aclService.canUpdate(incoming, user)) {
                changeType = SubmissionChangeType.UPDATE;
                submission = jpaIdentifiableRepository.merge(incoming);
            } else {
                throw new CreateAccessDeniedException("You have no right to send things here");
            }
        } else {
            if (aclService.canAddNew(incoming, user)) {
                changeType = SubmissionChangeType.CREATE;
                submission = jpaIdentifiableRepository.persist(incoming);
            } else {
                throw new UpdateAccessDeniedException("You have no right to send things here");
            }
        }
        // if failed, transaction should fail
        setOutboxEventRepository(submission);

        // publish event (for listener to do their job, i.e archive previous version to
        // data_submission_history after commit, set assignment state.
        eventPublisher.publishEvent(new SubmissionSavedEvent(submission.getId(),
            changeType, submission.getLockVersion()));

        return submission;
    }

    private void throwIfChangedTemplateVersion(@NonNull DataSubmission incoming, @NonNull DataSubmission existing) {
        if (!Objects.equals(incoming.getForm(), existing.getForm()) || (!Objects.equals(incoming.getFormVersion(), existing.getFormVersion()) &&
            !Objects.equals(incoming.getVersion(), existing.getVersion()))) {
            throw new DomainValidationException(ErrorCode.E4115, incoming.getFormVersion(), existing.getFormVersion());
        }
    }

    void setOutboxEventRepository(DataSubmission saved) {
        OutboxEvent evOk = new OutboxEvent();
        evOk.setAggregateType("DataSubmission");
        evOk.setAggregateId(saved.getId());
        evOk.setEventType("submission.saved");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("submissionId", saved.getId());
        payload.put("submissionVersion", saved.getLockVersion());
        evOk.setPayload(payload);
        evOk.setStatus(OutboxEventStatus.PENDING);
        evOk.setAttempts(0);
        evOk.setCreatedAt(Instant.now());
        evOk.setAvailableAt(Instant.now());
        outboxEventRepository.persist(evOk);
    }
}
