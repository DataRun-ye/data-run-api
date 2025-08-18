package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
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
import org.nmcpye.datarun.jpa.outbox.OutboxEvent;
import org.nmcpye.datarun.jpa.outbox.OutboxEventRepository;
import org.nmcpye.datarun.jpa.outbox.OutboxEventStatus;
import org.nmcpye.datarun.mongo.accessfilter.FormAccessService;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
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
    private final DataTemplateVersionRepository versionRepository;
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
        DataTemplateVersionRepository versionRepository,
        FormAccessService formAccessService,
        ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository) {
        super(repository, cacheManager, userAccessService);
        this.submissionDataProcessor = submissionDataProcessor;
        this.eventPublisher = eventPublisher;
        this.versionRepository = versionRepository;
        this.formAccessService = formAccessService;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public DataSubmission trySaveOrUpdate(DataSubmission incoming, CurrentUserDetails user) {
        checkAccessHook(incoming, user);
        submissionDataProcessor.processIncomingSubmission(incoming, objectMapper);
        Optional<DataSubmission> existingSubmission = findByIdOrUid(incoming);
        SubmissionChangeType changeType;
        DataSubmission submission;
        if (existingSubmission.isPresent()) {
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
        // data_submission_log after commit, set assignment state.
        eventPublisher.publishEvent(new SubmissionSavedEvent(submission.getId(),
            changeType, submission.getLockVersion()));

        return submission;
    }
//
//    @Transactional
//    @Override
//    public void preSaveHook(DataSubmission dto) {
//        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
//        if (user.getUsername().startsWith("test")) {
//            log.info("Test user '{}' - skipping persist", user.getUsername());
//            return dto;
//        }
//
//
//        return saved;
//    }

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

    public Optional<DataTemplateVersion> findByVersionNoOrVersionUid(String template, String version, Integer versionNum) {
        return Optional.ofNullable(version)
            .flatMap(versionRepository::findByUid)
            .or(() -> Optional.ofNullable(versionNum)
                .flatMap(v -> versionRepository.findByTemplateUidAndVersionNumber(template, versionNum)));
    }


    public void checkAccessHook(DataSubmission submission, CurrentUserDetails user) {
        if (Objects.isNull(submission.getForm()) || Objects.isNull(submission.getFormVersion())) {
            throw new IllegalQueryException("Submission `" + submission.getUid() + "` form property is not set");
        }

        if (!user.isSuper()) {
            if (!formAccessService.canSubmitData(submission.getForm())) {
                log.info("User {} cannot submit data", user.getUsername());
                throw new IllegalQueryException(ErrorCode.E1112, submission.getTeam());
            }
            if (Objects.isNull(submission.getTeam())) {
                throw new IllegalQueryException("Submission `" + submission.getUid() + "` team property is not set");
            }
            final var incomingTeam = submission.getTeam();
            if (!user.getUserTeamsUIDs().contains(incomingTeam)) {
                log.info("User {}, with team {} cannot submit data", user.getUsername(), incomingTeam);
                throw new IllegalQueryException(ErrorCode.E1112, submission.getTeam());
            }
        }
    }
}
